package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.eventhandler.HarvesterEventPublisher;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;

import java.util.List;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarvestRepositoryProcessorTest {

    @Test
    void shouldHarvestAllRepos() throws Exception {
        RepositoryService repositoryService = mock(RepositoryService.class);
        when(repositoryService.isHarvestingInProgress(any())).thenReturn(false);

        HarvesterService harvesterService = mock(HarvesterService.class);
        HarvesterEventPublisher harvesterEventPublisher = mock(HarvesterEventPublisher.class);
        List<String> reposToHarvest = List.of("repo1");
        HarvestRepositoryProcessor harvesterJob = new HarvestRepositoryProcessor(
                harvesterService,
                harvesterEventPublisher,
                reposToHarvest,
                repositoryService);

        harvesterJob.execute(mock(StepContribution.class), mock(ChunkContext.class));

        verify(harvesterService, times(1)).harvest(any(), any());
        verify(harvesterService).harvest(asRepo("repo1"), null);
    }

}
