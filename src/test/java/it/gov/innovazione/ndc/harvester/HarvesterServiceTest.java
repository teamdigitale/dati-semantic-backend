package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HarvesterServiceTest {
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
        harvesterService = new HarvesterService(agencyRepoService, List.of(harvester), tripleStoreRepository, metadataRepository);
    }

    @Test
    void shouldHarvestAssets() throws IOException {
        String repoUrl = "someRepoUri.git";
        String sanitizedRepoUrl = repoUrl.replace(".git", "");
        when(agencyRepoService.cloneRepo(sanitizedRepoUrl)).thenReturn(clonedRepoPath);

        harvesterService.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(harvester).harvest(sanitizedRepoUrl, clonedRepoPath);
    }

    @Test
    void shouldGiveUpOnRepoWhenGenericExceptionIsThrown() throws IOException {
        String repoUrl = "someRepoUri";

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        doThrow(new RuntimeException("Something else went wrong")).when(
                harvester).harvest(repoUrl, clonedRepoPath);

        assertThatThrownBy(() -> harvesterService.harvest(repoUrl))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Something else went wrong");

        verify(harvester).harvest(repoUrl, clonedRepoPath);
    }

    @Test
    void shouldClearNamedGraphAndMetadataBeforeProcessingData() throws IOException {
        String repoUrl = "someRepoUri";

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);

        harvesterService.harvest(repoUrl);

        InOrder order = inOrder(harvester, tripleStoreRepository, metadataRepository, harvester);
        order.verify(harvester).cleanUpBeforeHarvesting(repoUrl);
        order.verify(tripleStoreRepository).clearExistingNamedGraph(repoUrl);
        order.verify(metadataRepository).deleteByRepoUrl(repoUrl);
        order.verify(harvester).harvest(repoUrl, clonedRepoPath);
    }

    @Test
    void shouldCleanUpTemporaryFolderWithRepoAfterProcessing() throws IOException {
        String repoUrl = "someRepoUri";

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);

        harvesterService.harvest(repoUrl);

        verify(agencyRepoService).removeClonedRepo(clonedRepoPath);
    }

    @Test
    void shouldCleanUpTemporaryFolderWithRepoAfterFailure() throws IOException {
        String repoUrl = "someRepoUri";

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepoPath);
        doThrow(new RuntimeException("network disaster")).when(harvester)
                .harvest(repoUrl, clonedRepoPath);

        assertThatThrownBy(() -> harvesterService.harvest(repoUrl))
                .hasMessage("network disaster");

        verify(agencyRepoService).removeClonedRepo(clonedRepoPath);
    }
}
