package it.teamdigitale.ndc.harvester;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import it.teamdigitale.ndc.harvester.pathprocessors.OntologyPathProcessor;
import it.teamdigitale.ndc.harvester.pathprocessors.SchemaPathProcessor;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HarvesterServiceTest {
    @Mock
    AgencyRepositoryService agencyRepoService;
    @Mock
    ControlledVocabularyPathProcessor controlledVocabularyPathProcessor;
    @Mock
    OntologyPathProcessor ontologyPathProcessor;
    @Mock
    SchemaPathProcessor schemaPathProcessor;
    @Mock
    TripleStoreRepository tripleStoreRepository;
    @Mock
    Path clonedRepoPath;
    @Mock
    private SemanticAssetMetadataRepository metadataRepository;

    @InjectMocks
    HarvesterService harvester;


    @Test
    void shouldHarvestControlledVocabularies() throws IOException {
        String repoUrl = "someRepoUri.git";
        String sanitizedRepoUrl = repoUrl.replace(".git", "");
        CvPath path1 = CvPath.of("test1.ttl", "test1.csv");
        CvPath path2 = CvPath.of("test2.ttl", "test2.csv");
        when(agencyRepoService.cloneRepo(sanitizedRepoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getControlledVocabularyPaths(clonedRepoPath)).thenReturn(
            List.of(path1, path2));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getControlledVocabularyPaths(clonedRepoPath);
        verify(controlledVocabularyPathProcessor).process(sanitizedRepoUrl, path1);
        verify(controlledVocabularyPathProcessor).process(sanitizedRepoUrl, path2);
    }

    @Test
    void shouldHarvestOntologyFiles() throws IOException {
        String repoUrl = "someRepoUri";
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");
        SemanticAssetPath path2 = SemanticAssetPath.of("test2.ttl");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getOntologyPaths(clonedRepoPath)).thenReturn(List.of(path1, path2));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getOntologyPaths(clonedRepoPath);
        verify(ontologyPathProcessor).process(repoUrl, path1);
        verify(ontologyPathProcessor).process(repoUrl, path2);
    }

    @Test
    void shouldHarvestSchemaFiles() throws IOException {
        String repoUrl = "someRepoUri";
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");
        SemanticAssetPath path2 = SemanticAssetPath.of("test2.ttl");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getSchemaPaths(clonedRepoPath)).thenReturn(List.of(path1, path2));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getSchemaPaths(clonedRepoPath);
        verify(schemaPathProcessor).process(repoUrl, path1);
        verify(schemaPathProcessor).process(repoUrl, path2);
    }

    @Test
    void shouldMoveOnToNextOntologyIfProcessingOneFails() throws IOException {
        String repoUrl = "someRepoUri";
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");
        SemanticAssetPath path2 = SemanticAssetPath.of("test2.ttl");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getOntologyPaths(clonedRepoPath)).thenReturn(List.of(path1, path2));
        doThrow(new InvalidAssetException("Something went wrong")).when(ontologyPathProcessor)
            .process(repoUrl, path1);

        harvester.harvest(repoUrl);

        verify(ontologyPathProcessor).process(repoUrl, path1);
        verify(ontologyPathProcessor).process(repoUrl, path2);
    }

    @Test
    void shouldMoveOnToNextCvIfProcessingOneFails() throws IOException {
        String repoUrl = "someRepoUri";
        CvPath path1 = new CvPath("test1.ttl", "test1.csv");
        CvPath path2 = new CvPath("test2.ttl", "test2.csv");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getControlledVocabularyPaths(clonedRepoPath)).thenReturn(
            List.of(path1, path2));
        doThrow(new InvalidAssetException("Something went wrong")).when(
            controlledVocabularyPathProcessor).process(repoUrl, path1);

        harvester.harvest(repoUrl);

        verify(controlledVocabularyPathProcessor).process(repoUrl, path1);
        verify(controlledVocabularyPathProcessor).process(repoUrl, path2);
    }

    @Test
    void shouldGiveUpOnRepoWhenGenericExceptionIsThrown() throws IOException {
        String repoUrl = "someRepoUri";
        CvPath path1 = new CvPath("test1.ttl", "test1.csv");
        CvPath path2 = new CvPath("test2.ttl", "test2.csv");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getControlledVocabularyPaths(clonedRepoPath)).thenReturn(
            List.of(path1, path2));
        doThrow(new RuntimeException("Something else went wrong")).when(
            controlledVocabularyPathProcessor).process(repoUrl, path1);

        assertThatThrownBy(() -> harvester.harvest(repoUrl))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Something else went wrong");

        verify(controlledVocabularyPathProcessor).process(repoUrl, path1);
        verify(controlledVocabularyPathProcessor, never()).process(repoUrl, path2);
    }

    @Test
    void shouldClearNamedGraphAndMetadataBeforeProcessingData() throws IOException {
        String repoUrl = "someRepoUri";
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getOntologyPaths(clonedRepoPath)).thenReturn(List.of(path1));

        harvester.harvest(repoUrl);

        InOrder order = inOrder(tripleStoreRepository, metadataRepository, ontologyPathProcessor);
        order.verify(tripleStoreRepository).clearExistingNamedGraph(repoUrl);
        order.verify(metadataRepository).deleteByRepoUrl(repoUrl);
        order.verify(ontologyPathProcessor).process(repoUrl, path1);
    }

    @Test
    void shouldCleanUpTemporaryFolderWithRepoAfterProcessing() throws IOException {
        String repoUrl = "someRepoUri";
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getOntologyPaths(clonedRepoPath)).thenReturn(List.of(path1));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).removeClonedRepo(clonedRepoPath);
    }

    @Test
    void shouldCleanUpTemporaryFolderWithRepoAfterFailure() throws IOException {
        String repoUrl = "someRepoUri";
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        when(agencyRepoService.getOntologyPaths(clonedRepoPath)).thenReturn(List.of(path1));
        doThrow(new RuntimeException("network disaster")).when(ontologyPathProcessor)
            .process(repoUrl, path1);

        assertThatThrownBy(() -> harvester.harvest(repoUrl))
            .hasMessage("network disaster");

        verify(agencyRepoService).removeClonedRepo(clonedRepoPath);
    }
}
