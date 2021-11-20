package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import it.teamdigitale.ndc.harvester.exception.InvalidAssetFolderException;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyFolderScannerTest {
    private final Path folder = Path.of("/tmp/fake");

    @Mock
    FileUtils fileUtils;

    @InjectMocks
    ControlledVocabularyFolderScanner scanner;

    @Test
    void shouldFindTtlAndCsv() throws IOException {
        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of("cv.ttl"), Path.of("cv.csv")));

        List<CvPath> cvPaths = scanner.scanFolder(folder);

        assertThat(cvPaths).hasSize(1);
        assertThat(cvPaths).contains(new CvPath("cv.ttl", "cv.csv"));
    }

    @Test
    void shouldAllowForFolderWithJustTtl() throws IOException {
        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of("cv.ttl")));

        List<CvPath> cvPaths = scanner.scanFolder(folder);

        assertThat(cvPaths).hasSize(1);
        assertThat(cvPaths).contains(new CvPath("cv.ttl", null));
    }

    @ParameterizedTest
    @CsvSource({"cv.ttl,cv.csv", "cv.TTL,cv.CSV", "cv.ttl,cv.CSV", "cv.TTL,cv.csv"})
    void shouldFindOntologiesByCaseInsensitiveExtension(String ttlFileName, String csvFileName) throws IOException {
        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of(ttlFileName), Path.of(csvFileName)));

        List<CvPath> cvPaths = scanner.scanFolder(folder);

        assertThat(cvPaths).hasSize(1);
        assertThat(cvPaths).contains(new CvPath(ttlFileName, csvFileName));
    }

    @Test
    void shouldComplainForControlledVocabularyFolderWithMultipleTtlFiles() throws IOException {
        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of("the-real-cv.ttl"), Path.of("the-real-cv.csv"), Path.of("the-old-leftover-version.ttl")));

        assertThatThrownBy(() -> scanner.scanFolder(folder))
                .isInstanceOf(InvalidAssetFolderException.class);
    }
}