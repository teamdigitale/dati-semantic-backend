package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.scanners.ControlledVocabularyFolderScanner;
import it.gov.innovazione.ndc.harvester.scanners.ControlledVocabularyFolderScannerProperties;
import it.gov.innovazione.ndc.harvester.scanners.OntologyFolderScanner;
import it.gov.innovazione.ndc.harvester.scanners.OntologyFolderScannerProperties;
import it.gov.innovazione.ndc.harvester.scanners.SchemaFolderScanner;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import it.gov.innovazione.ndc.harvester.util.GitUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static it.gov.innovazione.ndc.harvester.AgencyRepositoryService.TEMP_DIR_PREFIX;
import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.gov.innovazione.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.gov.innovazione.ndc.harvester.SemanticAssetType.SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgencyRepositoryServiceTest {
    FileUtils fileUtils;
    GitUtils gitUtils;
    AgencyRepositoryService agencyRepoService;

    @BeforeEach
    public void setup() {
        fileUtils = mock(FileUtils.class);
        when(fileUtils.getLowerCaseFileName(any())).thenCallRealMethod();
        gitUtils = mock(GitUtils.class);
        OntologyFolderScanner ontologyScanner = new OntologyFolderScanner(fileUtils, OntologyFolderScannerProperties.forWords("aligns"));
        ControlledVocabularyFolderScanner cvScanner = new ControlledVocabularyFolderScanner(fileUtils, ControlledVocabularyFolderScannerProperties.forWords());
        SchemaFolderScanner schemaScanner = new SchemaFolderScanner(fileUtils);
        agencyRepoService = new AgencyRepositoryService(fileUtils, gitUtils, ontologyScanner,
                cvScanner, schemaScanner, AgencyRepositoryServiceProperties.forWords("scriptR2RML"));
    }

    @Test
    void shouldCloneTheRepoInTempDir() throws IOException {
        when(fileUtils.createTempDirectory(TEMP_DIR_PREFIX)).thenReturn(Path.of("temp"));

        Path clonedTempDir = agencyRepoService.cloneRepo("someURI");

        assertThat(clonedTempDir).isEqualTo(Path.of("temp"));
        verify(fileUtils).createTempDirectory(TEMP_DIR_PREFIX);
        verify(gitUtils).cloneRepo("someURI", new File("temp"), null);
    }

    /**
     * folder structure:
     * - VocabolariControllati
     * -- group1
     * --- cv1
     * ---- test1.ttl
     * ---- test1.csv
     * -- cv2
     * --- test2.ttl
     * --- test2.csv
     */
    @Test
    void shouldFindAllControlledVocabularies() throws IOException {
        CvPath expected1 = CvPath.of("test1.ttl", "test1.csv");
        CvPath expected2 = CvPath.of("test2.ttl", "test2.csv");
        Path cvFolder = Path.of("/temp/ndc-1", CONTROLLED_VOCABULARY.getFolderName());

        when(fileUtils.folderExists(cvFolder)).thenReturn(true);

        when(fileUtils.isDirectory(cvFolder)).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("group1"))).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("cv1"))).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("cv2"))).thenReturn(true);

        when(fileUtils.listContents(cvFolder))
                .thenReturn(List.of(Path.of("group1"), Path.of("cv2")));

        when(fileUtils.listContents(Path.of("group1")))
                .thenReturn(List.of(Path.of("cv1")));

        when(fileUtils.listContents(Path.of("cv2")))
                .thenReturn(List.of(Path.of("test2.csv"), Path.of("test2.ttl")));

        when(fileUtils.listContents(Path.of("cv1")))
                .thenReturn(List.of(Path.of("test1.csv"), Path.of("test1.ttl")));

        List<CvPath> cvPaths =
                agencyRepoService.getControlledVocabularyPaths(Path.of("/temp/ndc-1"));

        assertThat(cvPaths).hasSize(2);
        assertThat(cvPaths).containsAll(List.of(expected1, expected2));
    }

    @Test
    void shouldReturnEmptyListWhenControlledVocabularyFolderIsNotPresent() {
        Path cvFolder = Path.of("/temp/ndc-1", CONTROLLED_VOCABULARY.getFolderName());
        when(fileUtils.folderExists(cvFolder)).thenReturn(false);

        assertThat(agencyRepoService.getControlledVocabularyPaths(cvFolder)).isEmpty();
    }

    @Test
    void shouldFindAllOntologies() throws IOException {
        Path ontoFolder = Path.of("/temp/ndc-1", ONTOLOGY.getFolderName());
        String ontology1 = "group1/ot1/test1.ttl";
        String ontology2 = "ot2/test2.ttl";

        Path group1 =
                dir("group1",
                        dir("group1/ot1",
                                file(ontology1)
                        )
                );
        Path ot2 =
                dir("ot2",
                        file(ontology2)
                );

        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);
        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(group1, ot2));

        List<SemanticAssetPath> ontologyPaths =
                agencyRepoService.getOntologyPaths(Path.of("/temp/ndc-1"));

        assertThat(ontologyPaths).hasSize(2);
        assertThat(ontologyPaths).containsAll(List.of(SemanticAssetPath.of(ontology1), SemanticAssetPath.of(ontology2)));
    }

    @Test
    void shouldContinueScanningAfterEncounteringMalformedFolder() throws IOException {
        Path ontoFolder = Path.of("/temp/ndc-1", CONTROLLED_VOCABULARY.getFolderName());
        String cvTtl1 = "correct-vocab/test2.ttl";
        String cvCsv1 = "correct-vocab/test2.csv";

        Path group1 =
                dir("chaotic-vocab",
                        file("chaotic-vocab/test1.ttl"),
                        file("chaotic-vocab/test1.csv"),
                        file("chaotic-vocab/test2.ttl"),
                        file("chaotic-vocab/test2.csv")
                );
        Path ot2 =
                dir("correct-vocab",
                        file(cvTtl1),
                        file(cvCsv1)
                );

        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);
        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(group1, ot2));

        List<CvPath> cvPaths =
                agencyRepoService.getControlledVocabularyPaths(Path.of("/temp/ndc-1"));

        assertThat(cvPaths).hasSize(1);
        assertThat(cvPaths).contains(CvPath.of(cvTtl1, cvCsv1));
    }

    @Test
    void shouldFindAllSchemas() throws IOException {
        Path schemaFolder = Path.of("/temp/ndc-1", SCHEMA.getFolderName());
        String schema1 = "group1/sc1/index.ttl";
        String schema2 = "sc2/index.ttl";

        Path group1 =
                dir("group1",
                        dir("group1/sc1",
                                file(schema1)
                        )
                );
        Path sc2 =
                dir("sc2",
                        file(schema2)
                );

        when(fileUtils.folderExists(schemaFolder)).thenReturn(true);
        when(fileUtils.isDirectory(schemaFolder)).thenReturn(true);
        when(fileUtils.listContents(schemaFolder)).thenReturn(List.of(group1, sc2));

        List<SemanticAssetPath> schemaPaths =
                agencyRepoService.getSchemaPaths(Path.of("/temp/ndc-1"));

        assertThat(schemaPaths).hasSize(2);
        assertThat(schemaPaths).containsAll(
                List.of(SemanticAssetPath.of(schema1), SemanticAssetPath.of(schema2)));
    }

    @Test
    void shouldIgnoreFilesInNonLeafFolders() throws IOException {
        String root = "/temp/ndc-1";
        Path ontoFolder = Path.of(root, ONTOLOGY.getFolderName());
        String ontology1 = "group1/ot1/test1.ttl";
        String ontology2 = "ot2/test2.ttl";

        Path group1 =
                dir("group1",
                        dir("group1/ot1",
                                file(ontology1)
                        )
                );
        Path readme = file("README.md");
        Path ot2 =
                dir("ot2",
                        file(ontology2)
                );

        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);
        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(group1, readme, ot2));

        List<SemanticAssetPath> ontologyPaths =
                agencyRepoService.getOntologyPaths(Path.of(root));

        assertThat(ontologyPaths).hasSize(2);
        assertThat(ontologyPaths).containsAll(List.of(SemanticAssetPath.of(ontology1), SemanticAssetPath.of(ontology2)));
    }

    @Test
    void shouldIgnoreLeafFolderInSkipList() throws IOException {
        String root = "/temp/ndc-1";
        Path ontoFolder = Path.of(root, ONTOLOGY.getFolderName());
        String ontology1 = "ont1/test1.ttl";

        Path ont1 =
                dir("ont1",
                        file(ontology1),
                        dir("scriptR2RML",
                                file("test1_a.rml.ttl"),
                                file("test1_b.rml.ttl")
                        )
                );

        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);
        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(ont1));

        List<SemanticAssetPath> ontologyPaths =
                agencyRepoService.getOntologyPaths(Path.of(root));

        assertThat(ontologyPaths).containsExactly(SemanticAssetPath.of(ontology1));
    }

    @Test
    void shouldReturnEmptyListWhenOntologyFolderIsNotPresent() {
        Path ontologyFolder = Path.of("/temp/ndc-1", ONTOLOGY.getFolderName());
        when(fileUtils.folderExists(ontologyFolder)).thenReturn(false);

        assertThat(agencyRepoService.getOntologyPaths(ontologyFolder)).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenSchemaFolderIsNotPresent() {
        Path ontologyFolder = Path.of("/temp/ndc-1", SCHEMA.getFolderName());
        when(fileUtils.folderExists(ontologyFolder)).thenReturn(false);

        assertThat(agencyRepoService.getSchemaPaths(ontologyFolder)).isEmpty();
    }

    @Test
    void shouldConsiderLatestVersionPerAsset() throws IOException {
        String accoOntology = "ACCO/v2/acco2.ttl";
        String cpvOntology = "CPV/latest/cpv2.ttl";
        String root = "/temp/ndc-1";
        SemanticAssetPath expected1 = SemanticAssetPath.of(accoOntology);
        SemanticAssetPath expected2 = SemanticAssetPath.of(cpvOntology);
        Path ontoFolder = Path.of(root, ONTOLOGY.getFolderName());

        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);

        Path acco =
                dir("ACCO",
                        dir("ACCO/v1",
                                file("ACCO/v1/acco1.ttl")
                        ),
                        dir("ACCO/v2",
                                file(accoOntology)
                        )
                );

        Path cpv =
                dir("CPV",
                        dir("CPV/0.1",
                                file("CPV/0.1/cpv1.ttl")
                        ),
                        dir("CPV/latest",
                                file(cpvOntology)
                        )
                );

        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(acco, cpv));

        List<SemanticAssetPath> paths = agencyRepoService.getOntologyPaths(Path.of(root));

        assertThat(paths).hasSize(2);
        assertThat(paths).containsAll(List.of(expected1, expected2));
    }

    @Test
    void shouldConsiderLatestVersionPerWholeRepo() throws IOException {
        SemanticAssetPath expected1 = SemanticAssetPath.of("2.0/ACCO/acco2.ttl");
        SemanticAssetPath expected2 = SemanticAssetPath.of("2.0/CPV/cpv2.ttl");
        String root = "/temp/ndc-1";
        Path ontoFolder = Path.of(root, ONTOLOGY.getFolderName());

        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);

        Path v1 =
                dir("v1",
                        dir("v1/ACCO",
                                file("v1/ACCO/acco1.ttl")
                        ),
                        dir("v1/CPV",
                                file("v1/CPV/cpv1.ttl")
                        )
                );

        Path v2 =
                dir("2.0",
                        dir("2.0/ACCO",
                                file("2.0/ACCO/acco2.ttl")
                        ),
                        dir("2.0/CPV",
                                file("2.0/CPV/cpv2.ttl")
                        )
                );

        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(v1, v2));

        List<SemanticAssetPath> paths = agencyRepoService.getOntologyPaths(Path.of(root));

        assertThat(paths).hasSize(2);
        assertThat(paths).containsAll(List.of(expected1, expected2));
    }

    @Test
    void shouldConsiderLatestVersionAndProcessNonVersionedFolders() throws IOException {
        SemanticAssetPath expected1 = SemanticAssetPath.of("2.0/ACCO/acco2.ttl");
        SemanticAssetPath expected2 = SemanticAssetPath.of("2.0/CPV/cpv2.ttl");
        SemanticAssetPath expected3 = SemanticAssetPath.of("non-versioned/FIT/fit.ttl");
        String root = "/temp/ndc-1";
        Path ontoFolder = Path.of(root, ONTOLOGY.getFolderName());

        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);

        Path v1 =
                dir("v1",
                        dir("v1/ACCO",
                                file("v1/ACCO/acco1.ttl")
                        ),
                        dir("v1/CPV",
                                file("v1/CPV/cpv1.ttl")
                        )
                );

        Path v2 =
                dir("2.0",
                        dir("2.0/ACCO",
                                file("2.0/ACCO/acco2.ttl")
                        ),
                        dir("2.0/CPV",
                                file("2.0/CPV/cpv2.ttl")
                        )
                );

        Path nonVersioned = dir("non-versioned",
                dir("non-versioned/FIT",
                        file("non-versioned/FIT/fit.ttl")
                )
        );

        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(v1, v2, nonVersioned));

        List<SemanticAssetPath> paths = agencyRepoService.getOntologyPaths(Path.of(root));

        assertThat(paths).hasSize(3);
        assertThat(paths).containsAll(List.of(expected1, expected2, expected3));
    }

    @Test
    void shouldConsiderLatestWithinLatest() throws IOException {
        String root = "/temp/ndc-1";
        Path ontoFolder = Path.of(root, ONTOLOGY.getFolderName());

        when(fileUtils.folderExists(ontoFolder)).thenReturn(true);
        when(fileUtils.isDirectory(ontoFolder)).thenReturn(true);

        Path acco =
                dir("ACCO",
                        dir("ACCO/v1",
                                file("ACCO/v1/acco1.ttl")
                        ),
                        dir("ACCO/v2",
                                dir("ACCO/v2/v2.1",
                                        file("ACCO/v2/v2.1/acco.ttl")
                                ),
                                dir("ACCO/v2/v2.2",
                                        file("ACCO/v2/v2.2/acco.ttl")
                                ),
                                dir("ACCO/v2/v2.3",
                                        file("ACCO/v2/v2.3/acco.ttl")
                                )
                        )
                );

        when(fileUtils.listContents(ontoFolder)).thenReturn(List.of(acco));

        List<SemanticAssetPath> paths = agencyRepoService.getOntologyPaths(Path.of(root));

        assertThat(paths).hasSize(1);
        assertThat(paths).containsAll(List.of(SemanticAssetPath.of("ACCO/v2/v2.3/acco.ttl")));
    }

    @Test
    void shouldCleanUpRepoFolder() throws IOException {
        Path clonedRepoPath = mock(Path.class);

        agencyRepoService.removeClonedRepo(clonedRepoPath);

        verify(fileUtils).removeDirectory(clonedRepoPath);
    }

    private Path dir(String folderName, Path... children) throws IOException {
        Path cpvLatest = Path.of(folderName);
        when(fileUtils.isDirectory(cpvLatest)).thenReturn(true);
        when(fileUtils.listContents(cpvLatest)).thenReturn(List.of(children));
        return cpvLatest;
    }

    private Path file(String fileName) {
        Path p = Path.of(fileName);
        when(fileUtils.isDirectory(p)).thenReturn(false);
        return p;
    }

}
