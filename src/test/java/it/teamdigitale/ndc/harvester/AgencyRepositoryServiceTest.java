package it.teamdigitale.ndc.harvester;

import static it.teamdigitale.ndc.harvester.AgencyRepositoryService.TEMP_DIR_PREFIX;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.scanners.ControlledVocabularyFolderScanner;
import it.teamdigitale.ndc.harvester.scanners.OntologyFolderScanner;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import it.teamdigitale.ndc.harvester.util.GitUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AgencyRepositoryServiceTest {
    FileUtils fileUtils;
    GitUtils gitUtils;
    AgencyRepositoryService agencyRepoService;

    @BeforeEach
    public void setup() {
        fileUtils = mock(FileUtils.class);
        gitUtils = mock(GitUtils.class);
        OntologyFolderScanner ontologyScanner = new OntologyFolderScanner(fileUtils);
        ControlledVocabularyFolderScanner cvScanner = new ControlledVocabularyFolderScanner(fileUtils);
        agencyRepoService = new AgencyRepositoryService(fileUtils, gitUtils, ontologyScanner, cvScanner);
    }

    @Test
    void shouldCloneTheRepoInTempDir() throws GitAPIException, IOException {
        when(fileUtils.createTempDirectory(TEMP_DIR_PREFIX)).thenReturn(Path.of("temp"));

        Path clonedTempDir = agencyRepoService.cloneRepo("someURI");

        assertThat(clonedTempDir).isEqualTo(Path.of("temp"));
        verify(fileUtils).createTempDirectory(TEMP_DIR_PREFIX);
        verify(gitUtils).cloneRepo("someURI", new File("temp"));
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

    /**
     * folder structure:
     * - Ontologie
     * -- group1
     * --- ot1
     * ---- test1.ttl
     * -- ot2
     * --- test2.ttl
     */
    @Test
    void shouldFindAllOntologies() throws IOException {
        Path folder = Path.of("/temp/ndc-1", ONTOLOGY.getFolderName());
        String ontology1 = "test1.ttl";
        String ontology2 = "test2.ttl";

        when(fileUtils.folderExists(folder)).thenReturn(true);

        when(fileUtils.isDirectory(folder)).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("group1"))).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("ot1"))).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("ot2"))).thenReturn(true);

        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of("group1"), Path.of("ot2")));

        when(fileUtils.listContents(Path.of("group1")))
                .thenReturn(List.of(Path.of("ot1")));

        when(fileUtils.listContents(Path.of("ot2")))
                .thenReturn(List.of(Path.of(ontology2)));

        when(fileUtils.listContents(Path.of("ot1")))
                .thenReturn(List.of(Path.of(ontology1)));

        List<SemanticAssetPath> ontologyPaths =
                agencyRepoService.getOntologyPaths(Path.of("/temp/ndc-1"));

        assertThat(ontologyPaths).hasSize(2);
        assertThat(ontologyPaths).containsAll(List.of(new SemanticAssetPath(ontology1), new SemanticAssetPath(ontology2)));
    }

    /**
     * folder structure:
     * - Ontologie
     * -- group1
     * --- ot1
     * ---- test1.ttl
     * -- README.md
     * -- ot2
     * --- test2.ttl
     */
    @Test
    void shouldIgnoreFilesInNonLeafFolders() throws IOException {
        Path folder = Path.of("/temp/ndc-1", ONTOLOGY.getFolderName());
        String ontology1 = "test1.ttl";
        String ontology2 = "test2.ttl";

        when(fileUtils.folderExists(folder)).thenReturn(true);

        when(fileUtils.isDirectory(folder)).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("group1"))).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("ot1"))).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("ot2"))).thenReturn(true);
        when(fileUtils.isDirectory(Path.of("README.md"))).thenReturn(false);

        when(fileUtils.listContents(folder))
                .thenReturn(List.of(Path.of("group1"), Path.of("README.md"), Path.of("ot2")));

        when(fileUtils.listContents(Path.of("group1")))
                .thenReturn(List.of(Path.of("ot1")));

        when(fileUtils.listContents(Path.of("README.md")))
                .thenThrow(new IOException("There's no further content inside README.md..."));

        when(fileUtils.listContents(Path.of("ot2")))
                .thenReturn(List.of(Path.of(ontology2)));

        when(fileUtils.listContents(Path.of("ot1")))
                .thenReturn(List.of(Path.of(ontology1)));

        List<SemanticAssetPath> ontologyPaths =
                agencyRepoService.getOntologyPaths(Path.of("/temp/ndc-1"));

        assertThat(ontologyPaths).hasSize(2);
        assertThat(ontologyPaths).containsAll(List.of(new SemanticAssetPath(ontology1), new SemanticAssetPath(ontology2)));
    }

    @Test
    void shouldReturnEmptyListWhenOntologyFolderIsNotPresent() {
        Path ontologyFolder = Path.of("/temp/ndc-1", ONTOLOGY.getFolderName());
        when(fileUtils.folderExists(ontologyFolder)).thenReturn(false);

        assertThat(agencyRepoService.getOntologyPaths(ontologyFolder)).isEmpty();
    }
}
