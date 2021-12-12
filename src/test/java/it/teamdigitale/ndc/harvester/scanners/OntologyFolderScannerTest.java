package it.teamdigitale.ndc.harvester.scanners;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldIgnoreExamples() throws IOException {
        mockFolderToContain("onto.ttl", "examples.ttl", "aligns-onto.ttl", "onto-aligns.ttl");

        List<SemanticAssetPath> ontologyPaths = scanner.scanFolder(folder);

        assertThat(ontologyPaths).containsOnly(SemanticAssetPath.of("onto.ttl"));
    }
}