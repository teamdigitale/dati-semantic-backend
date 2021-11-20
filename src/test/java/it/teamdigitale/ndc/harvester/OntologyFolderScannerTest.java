package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OntologyFolderScannerTest {
    private final Path folder = Path.of("/tmp/fake");

    @Mock
    FileUtils fileUtils;

    @InjectMocks
    OntologyFolderScanner scanner;

    @Test
    void shouldFindAllOntologiesAndIgnoreCsvs() throws IOException {
        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of("onto.ttl"), Path.of("onto.csv")));

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).hasSize(1);
        assertThat(ontologyPaths).contains(new SemanticAssetPath("onto.ttl"));
    }

    @ParameterizedTest
    @CsvSource({"onto.ttl,onto-aligns.ttl", "onto-aligns.ttl,onto.ttl"})
    void shouldFindAllOntologiesAndIgnoreAligns(String firstTtl, String secondTtl) throws IOException {
        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of(firstTtl), Path.of(secondTtl)));

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).hasSize(1);
        assertThat(ontologyPaths).contains(new SemanticAssetPath("onto.ttl"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"onto.ttl", "ONTO.TTL", "very-unlikely.Ttl"})
    void shouldFindOntologiesByCaseInsensitiveExtension(String fileName) throws IOException {
        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of(fileName)));

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).hasSize(1);
        assertThat(ontologyPaths).contains(new SemanticAssetPath(fileName));
    }
}