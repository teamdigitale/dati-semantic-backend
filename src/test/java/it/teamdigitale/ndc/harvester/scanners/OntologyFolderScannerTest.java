package it.teamdigitale.ndc.harvester.scanners;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OntologyFolderScannerTest extends BaseFolderScannerTest {
    private OntologyFolderScanner scanner;

    @BeforeEach
    void setupScanner() {
        scanner = new OntologyFolderScanner(fileUtils, OntologyFolderScannerProperties.forWords("aligns", "example"));
    }

    @Test
    void shouldFindAllOntologiesAndIgnoreCsvs() throws IOException {
        mockFolderToContain("onto.ttl", "onto.csv");

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).containsOnly(SemanticAssetPath.of("onto.ttl"));
    }

    @Test
    void shouldComplainForMissingProperties() {
        assertThatThrownBy(() -> new OntologyFolderScanner(fileUtils, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("properties");
    }

    @ParameterizedTest
    @CsvSource({"onto.ttl,onto-aligns.ttl", "onto-aligns.ttl,onto.ttl"})
    void shouldFindAllOntologiesAndIgnoreAligns(String firstTtl, String secondTtl) throws IOException {
        mockFolderToContain(firstTtl, secondTtl);

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).containsOnly(SemanticAssetPath.of("onto.ttl"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"onto.ttl", "ONTO.TTL", "very-unlikely.Ttl"})
    void shouldFindOntologiesByCaseInsensitiveExtension(String fileName) throws IOException {
        mockFolderToContain(fileName);

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).containsOnly(SemanticAssetPath.of(fileName));
    }

    @Test
    void shouldIgnoreExamplesAndAligns() throws IOException {
        mockFolderToContain("onto.ttl", "examples.ttl", "aligns-onto.ttl", "onto-aligns.ttl");

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).containsOnly(SemanticAssetPath.of("onto.ttl"));
    }

    @Test
    void shouldPickMultipleTurtlesInSameFolder() throws IOException {
        mockFolderToContain("onto1.ttl", "examples.ttl", "onto2.ttl");

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).containsOnly(SemanticAssetPath.of("onto1.ttl"), SemanticAssetPath.of("onto2.ttl"));
    }

    @Test
    void shouldSupportNullSkipWords() throws IOException {
        scanWithSkipWords(null);
    }

    @Test
    void shouldSupportEmptySkipWords() throws IOException {
        scanWithSkipWords(Collections.emptyList());
    }

    @Test
    void shouldComplainForShortSkipWords() throws IOException {
        assertThatThrownBy(() -> new OntologyFolderScanner(fileUtils, OntologyFolderScannerProperties.forWords("example", "-")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("long");
    }

    private void scanWithSkipWords(List<String> skipWords) throws IOException {
        OntologyFolderScanner scanner = new OntologyFolderScanner(fileUtils, new OntologyFolderScannerProperties(skipWords));
        mockFolderToContain("example.ttl");

        List<SemanticAssetPath> paths = scanner.scanFolder(folder);

        assertThat(paths.contains(SemanticAssetPath.of("example.ttl")));
    }
}