package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}