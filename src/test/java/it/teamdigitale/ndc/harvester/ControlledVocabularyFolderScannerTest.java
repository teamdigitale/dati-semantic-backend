package it.teamdigitale.ndc.harvester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetFolderException;
import it.teamdigitale.ndc.harvester.model.CvPath;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyFolderScannerTest extends BaseFolderScannerTest {

    @InjectMocks
    ControlledVocabularyFolderScanner scanner;

    @Test
    void shouldFindTtlAndCsv() throws IOException {
        mockFolderToContain("cv.ttl", "cv.csv");

        List<CvPath> cvPaths = scanner.scanFolder(folder);

        assertThat(cvPaths).containsOnly(new CvPath("cv.ttl", "cv.csv"));
    }

    @Test
    void shouldAllowForFolderWithJustTtl() throws IOException {
        mockFolderToContain("cv.ttl");

        List<CvPath> cvPaths = scanner.scanFolder(folder);

        assertThat(cvPaths).containsOnly(new CvPath("cv.ttl", null));
    }

    @Test
    void shouldReturnEmptyResultsWhenTtlIsNotPresentInFolder() throws IOException {
        List<CvPath> cvPaths = scanner.scanFolder(folder);

        assertThat(cvPaths).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"cv.ttl,cv.csv", "cv.TTL,cv.CSV", "cv.ttl,cv.CSV", "cv.TTL,cv.csv"})
    void shouldFindOntologiesByCaseInsensitiveExtension(String ttlFileName, String csvFileName)
        throws IOException {
        mockFolderToContain(ttlFileName, csvFileName);

        List<CvPath> cvPaths = scanner.scanFolder(folder);

        assertThat(cvPaths).containsOnly(new CvPath(ttlFileName, csvFileName));
    }

    @Test
    void shouldComplainForControlledVocabularyFolderWithMultipleTtlFiles() throws IOException {
        mockFolderToContain("the-real-cv.ttl", "the-real-cv.csv", "the-old-leftover-version.ttl");

        assertThatThrownBy(() -> scanner.scanFolder(folder))
            .isInstanceOf(InvalidAssetFolderException.class);
    }

    @Test
    void shouldComplainForControlledVocabularyFolderWithMultipleCsvFiles() throws IOException {
        mockFolderToContain("the-real-cv.ttl", "the-real-cv.csv", "the-experimental-cv.csv");

        assertThatThrownBy(() -> scanner.scanFolder(folder))
            .isInstanceOf(InvalidAssetFolderException.class);
    }
}