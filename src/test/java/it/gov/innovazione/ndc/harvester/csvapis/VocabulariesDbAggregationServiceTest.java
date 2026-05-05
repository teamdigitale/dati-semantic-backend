package it.gov.innovazione.ndc.harvester.csvapis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabulariesDbAggregationServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @TempDir
    Path tempDir;

    Path aggregatePath;
    Path workDir;
    Path hashFile;
    HarvestAssetStateService stateService;
    VocabulariesDbMerger merger;

    @BeforeEach
    void setup() {
        aggregatePath = tempDir.resolve("vocab.db");
        workDir = tempDir.resolve("work");
        hashFile = Path.of(aggregatePath.toString() + ".aggregate-hash");
        stateService = new HarvestAssetStateService(jdbcTemplate, workDir.toString());
        merger = mock(VocabulariesDbMerger.class);
    }

    @Test
    void mergesAndPublishesWhenNoPreviousAggregateExists() throws IOException, SQLException {
        Path source = createWorkDirEntry("agencyA", "cities", "src-A");
        stubJdbcReturning(new VocabulariesDbAggregationService.AssetEntry("agencyA", "cities", "hash-A"));
        stubMergerToCreateOutput();

        newService().aggregateIfNeeded();

        verify(merger).merge(eq(List.of(source)), any(Path.class));
        assertThat(aggregatePath).exists();
        assertThat(hashFile).exists();
        assertThat(Files.readString(hashFile)).isNotBlank();
    }

    @Test
    void skipsMergeWhenAggregateHashIsUnchanged() throws IOException, SQLException {
        createWorkDirEntry("agencyA", "cities", "src-A");
        stubJdbcReturning(new VocabulariesDbAggregationService.AssetEntry("agencyA", "cities", "hash-A"));

        // First run produces both files.
        stubMergerToCreateOutput();
        newService().aggregateIfNeeded();
        verify(merger).merge(anyList(), any(Path.class));

        // Second run: same input → skip merge.
        VocabulariesDbMerger merger2 = mock(VocabulariesDbMerger.class);
        new VocabulariesDbAggregationService(jdbcTemplate, merger2, stateService, aggregatePath.toString())
                .aggregateIfNeeded();

        verify(merger2, never()).merge(anyList(), any(Path.class));
    }

    @Test
    void rebuildsWhenAssetHashChanges() throws IOException, SQLException {
        createWorkDirEntry("agencyA", "cities", "src-A");
        stubJdbcReturning(new VocabulariesDbAggregationService.AssetEntry("agencyA", "cities", "hash-A"));
        stubMergerToCreateOutput();
        newService().aggregateIfNeeded();

        // Different source hash for the same asset.
        stubJdbcReturning(new VocabulariesDbAggregationService.AssetEntry("agencyA", "cities", "hash-A-v2"));
        VocabulariesDbMerger merger2 = mock(VocabulariesDbMerger.class);
        doAnswer(inv -> {
            Path output = inv.getArgument(1);
            Files.write(output, "merged-v2".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(merger2).merge(anyList(), any(Path.class));

        new VocabulariesDbAggregationService(jdbcTemplate, merger2, stateService, aggregatePath.toString())
                .aggregateIfNeeded();

        verify(merger2).merge(anyList(), any(Path.class));
        assertThat(Files.readString(aggregatePath)).isEqualTo("merged-v2");
    }

    @Test
    void skipsAssetsWhoseWorkDirEntryIsMissing() throws IOException, SQLException {
        // Only one of the two referenced assets actually has a file on disk.
        createWorkDirEntry("agencyA", "cities", "src-A");
        stubJdbcReturning(
                new VocabulariesDbAggregationService.AssetEntry("agencyA", "cities", "hash-A"),
                new VocabulariesDbAggregationService.AssetEntry("agencyMissing", "absent", "hash-X"));
        stubMergerToCreateOutput();

        newService().aggregateIfNeeded();

        verify(merger).merge(eq(List.of(workDir.resolve("agencyA").resolve("cities.db"))), any(Path.class));
    }

    @Test
    void doesNothingWhenStateIsEmpty() throws IOException, SQLException {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        newService().aggregateIfNeeded();

        verify(merger, never()).merge(anyList(), any(Path.class));
        assertThat(aggregatePath).doesNotExist();
    }

    private VocabulariesDbAggregationService newService() {
        return new VocabulariesDbAggregationService(
                jdbcTemplate, merger, stateService, aggregatePath.toString());
    }

    private Path createWorkDirEntry(String agency, String concept, String content) throws IOException {
        Path target = workDir.resolve(agency).resolve(concept + ".db");
        Files.createDirectories(java.util.Objects.requireNonNull(target.getParent()));
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        return target;
    }

    @SuppressWarnings("unchecked")
    private void stubJdbcReturning(VocabulariesDbAggregationService.AssetEntry... entries) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(entries));
    }

    private void stubMergerToCreateOutput() throws IOException, SQLException {
        doAnswer(inv -> {
            Path output = inv.getArgument(1);
            Files.write(output, "merged".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(merger).merge(anyList(), any(Path.class));
    }
}
