package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HarvesterServiceTest {
    @Mock
    private AgencyRepositoryService agencyRepoService;
    @Mock
    private TripleStoreRepository tripleStoreRepository;
    @Mock
    private Path clonedRepoPath;
    @Mock
    private SemanticAssetMetadataRepository metadataRepository;
    @Mock
    private SemanticAssetHarvester harvester;

    private HarvesterService harvesterService;

    @BeforeEach
    void setUp() {
        harvesterService = new HarvesterService(
                agencyRepoService,
                List.of(harvester),
                tripleStoreRepository,
                metadataRepository);
    }

    @Test
    void shouldHarvestAssets() throws IOException {
        String repoUrl = "someRepoUri.git";
        String sanitizedRepoUrl = repoUrl.replace(".git", "");
        when(agencyRepoService.cloneRepo(sanitizedRepoUrl, null)).thenReturn(clonedRepoPath);

        harvesterService.harvest(asRepo(repoUrl));

        verify(agencyRepoService).cloneRepo("someRepoUri", null);

        ArgumentCaptor<Repository> repoCaptor = ArgumentCaptor.forClass(Repository.class);
        verify(harvester).harvest(repoCaptor.capture(), any());
        assertEquals(sanitizedRepoUrl, repoCaptor.getValue().getUrl());
    }

    @Test
    void shouldGiveUpOnRepoWhenGenericExceptionIsThrown() throws IOException {
        String repoUrl = "someRepoUri";
        Repository repo = asRepo(repoUrl);

        when(agencyRepoService.cloneRepo(repoUrl, null)).thenReturn(clonedRepoPath);
        doThrow(new RuntimeException("Something else went wrong")).when(
                harvester).harvest(repo, clonedRepoPath);

        assertThatThrownBy(() -> harvesterService.harvest(asRepo(repoUrl)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Something else went wrong");

        verify(harvester).harvest(repo, clonedRepoPath);
    }

    @Test
    void shouldClearNamedGraphAndMetadataBeforeProcessingData() throws IOException {
        String repoUrl = "someRepoUri";
        Repository repo = asRepo(repoUrl);

        when(agencyRepoService.cloneRepo(repoUrl, null)).thenReturn(clonedRepoPath);

        harvesterService.harvest(asRepo(repoUrl));

        InOrder order = inOrder(harvester, tripleStoreRepository, metadataRepository, harvester);
        order.verify(harvester).cleanUpBeforeHarvesting(repoUrl);
        order.verify(tripleStoreRepository).clearExistingNamedGraph(repoUrl);
        order.verify(metadataRepository).deleteByRepoUrl(repoUrl);
        order.verify(harvester).harvest(repo, clonedRepoPath);
    }

    @Test
    void shouldCleanUpTemporaryFolderWithRepoAfterProcessing() throws IOException {
        String repoUrl = "someRepoUri";

        when(agencyRepoService.cloneRepo(repoUrl, null)).thenReturn(clonedRepoPath);

        harvesterService.harvest(asRepo(repoUrl));

        verify(agencyRepoService).removeClonedRepo(clonedRepoPath);
    }

    @Test
    void shouldCleanUpTemporaryFolderWithRepoAfterFailure() throws IOException {
        String repoUrl = "someRepoUri";
        Repository repo = asRepo(repoUrl);

        when(agencyRepoService.cloneRepo(repoUrl, null)).thenReturn(clonedRepoPath);
        doThrow(new RuntimeException("network disaster")).when(harvester)
                .harvest(repo, clonedRepoPath);

        assertThatThrownBy(() -> harvesterService.harvest(asRepo(repoUrl)))
                .hasMessage("network disaster");

        verify(agencyRepoService).removeClonedRepo(clonedRepoPath);
    }

    @Test
    void shouldClearRepoData() {
        String repoUrl = "someRepoUri.git";
        String sanitizedRepoUrl = repoUrl.replace(".git", "");

        harvesterService.clear(repoUrl);

        InOrder order = inOrder(harvester, tripleStoreRepository, metadataRepository, harvester);
        order.verify(harvester).cleanUpBeforeHarvesting(sanitizedRepoUrl);
        order.verify(tripleStoreRepository).clearExistingNamedGraph(sanitizedRepoUrl);
        order.verify(metadataRepository).deleteByRepoUrl(sanitizedRepoUrl);
    }

    @Test
    void shouldPropagateExceptionWhileClearingRepo() {
        String repoUrl = "someRepoUri.git";
        String sanitizedRepoUrl = "someRepoUri";


        RuntimeException exception = new RuntimeException("Something bad happened!");
        doThrow(exception).when(harvester).cleanUpBeforeHarvesting(sanitizedRepoUrl);

        assertThatThrownBy(() -> harvesterService.clear(repoUrl))
                .isSameAs(exception);

    }
}
