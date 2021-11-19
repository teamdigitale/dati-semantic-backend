package it.teamdigitale.ndc.harvester;

import static it.teamdigitale.ndc.harvester.AgencyRepositoryService.CV_FOLDER;
import static it.teamdigitale.ndc.harvester.AgencyRepositoryService.ONTOLOGY_FOLDER;
import static it.teamdigitale.ndc.harvester.AgencyRepositoryService.TEMP_DIR_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
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
        agencyRepoService = new AgencyRepositoryService(fileUtils, gitUtils);
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
        CvPath expected1 = CvPath.of("test1.csv", "test1.ttl");
        CvPath expected2 = CvPath.of("test2.csv", "test2.ttl");
        Path cvFolder = Path.of("/temp/ndc-1", CV_FOLDER);

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
        Path cvFolder = Path.of("/temp/ndc-1", CV_FOLDER);
        when(fileUtils.folderExists(cvFolder)).thenReturn(false);

        assertThat(agencyRepoService.getControlledVocabularyPaths(cvFolder)).isEmpty();
    }

    /**
     * folder structure:
     * - Ontologie
     * -- group1
     * --- ot1
     * ---- test1.ttl
     * ---- test1-aligns.ttl
     * -- ot2
     * --- test2.ttl
     */
    @Test
    void shouldFindAllOntologies() throws IOException {
        SemanticAssetPath expected1 =  new SemanticAssetPath("test1.ttl");
        SemanticAssetPath expected2 =  new SemanticAssetPath("test2.ttl");
        Path folder = Path.of("/temp/ndc-1", ONTOLOGY_FOLDER);

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
                .thenReturn(List.of(Path.of("test2.ttl")));

        when(fileUtils.listContents(Path.of("ot1")))
                .thenReturn(List.of(Path.of("test1.ttl")));

        List<SemanticAssetPath> ontologyPaths =
                agencyRepoService.getOntologyPaths(Path.of("/temp/ndc-1"));

        assertThat(ontologyPaths).hasSize(2);
        assertThat(ontologyPaths).containsAll(List.of(expected1, expected2));
    }

    @Test
    void shouldReturnEmptyListWhenOntologyFolderIsNotPresent() {
        Path ontologyFolder = Path.of("/temp/ndc-1", ONTOLOGY_FOLDER);
        when(fileUtils.folderExists(ontologyFolder)).thenReturn(false);

        assertThat(agencyRepoService.getOntologyPaths(ontologyFolder)).isEmpty();
    }
}
