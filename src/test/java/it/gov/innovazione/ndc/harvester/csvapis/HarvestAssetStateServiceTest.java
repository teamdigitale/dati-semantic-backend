package it.gov.innovazione.ndc.harvester.csvapis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HarvestAssetStateServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @TempDir
    Path workDir;

    HarvestAssetStateService service;

    @BeforeEach
    void setup() {
        service = new HarvestAssetStateService(jdbcTemplate, workDir.toString());
    }

    @Test
    void recordsDbDetectedRowAndCopiesFileIntoWorkDir(@TempDir Path source) throws IOException {
        Path sourceDb = source.resolve("agencyA-cities.db");
        byte[] payload = "fake-sqlite-content".getBytes(StandardCharsets.UTF_8);
        Files.write(sourceDb, payload);
        Instant detectedAt = Instant.parse("2026-05-05T10:00:00Z");

        service.recordDbDetected(
                "run-id", "https://example/repo", "agencyA", "cities",
                "abc123", sourceDb, detectedAt);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(contains("INSERT INTO HARVEST_ASSET_STATE"), argsCaptor.capture());

        Object[] actual = (Object[]) argsCaptor.getAllValues().toArray()[0];
        if (actual.length == 1 && actual[0] instanceof Object[]) {
            actual = (Object[]) actual[0];
        }
        assertThat(actual).hasSize(7);
        assertThat(actual[1]).isEqualTo("run-id");
        assertThat(actual[2]).isEqualTo("https://example/repo");
        assertThat(actual[3]).isEqualTo("agencyA");
        assertThat(actual[4]).isEqualTo("cities");
        assertThat(actual[5]).isEqualTo("abc123");
        assertThat(actual[6]).isEqualTo(Timestamp.from(detectedAt));

        Path target = workDir.resolve("agencyA").resolve("cities.db");
        assertThat(target).exists();
        assertThat(Files.readAllBytes(target)).isEqualTo(payload);
    }

    @Test
    void copyOverwritesPreviousWorkDirEntry(@TempDir Path source) throws IOException {
        Path target = workDir.resolve("agencyA").resolve("cities.db");
        Files.createDirectories(java.util.Objects.requireNonNull(target.getParent()));
        Files.write(target, "stale".getBytes(StandardCharsets.UTF_8));

        Path sourceDb = source.resolve("cities.db");
        Files.write(sourceDb, "fresh".getBytes(StandardCharsets.UTF_8));

        service.recordDbDetected(
                "run", "repo", "agencyA", "cities",
                "h", sourceDb, Instant.now());

        assertThat(Files.readAllBytes(target)).isEqualTo("fresh".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void sanitizesIdentifiersForFilesystem(@TempDir Path source) throws IOException {
        Path sourceDb = source.resolve("a.db");
        Files.write(sourceDb, new byte[]{1});

        service.recordDbDetected(
                "run", "repo", "agency/with:slash", "concept with spaces",
                "h", sourceDb, Instant.now());

        Path expected = workDir.resolve("agency_with_slash").resolve("concept_with_spaces.db");
        assertThat(expected).exists();
    }
}
