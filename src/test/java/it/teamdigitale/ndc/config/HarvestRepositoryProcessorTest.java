package it.teamdigitale.ndc.config;

import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.repository.HarvestJobException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class HarvestRepositoryProcessorTest {

    @Test
    void shouldHarvestAllRepos() throws Exception {
        HarvesterService harvesterService = mock(HarvesterService.class);
        List<String> reposToHarvest = List.of("repo1", "repo2");
        HarvestRepositoryProcessor harvesterJob = new HarvestRepositoryProcessor(harvesterService, reposToHarvest);

        harvesterJob.execute(mock(StepContribution.class), mock(ChunkContext.class));

        verify(harvesterService, times(2)).harvest(any());
        verify(harvesterService).harvest("repo1");
        verify(harvesterService).harvest("repo2");
    }

    @Test
    void shouldHarvestAllReposContinueToNextInCaseOfFailure() throws Exception {
        HarvesterService harvesterService = mock(HarvesterService.class);
        List<String> reposToHarvest = List.of("repo1", "repo2");
        doThrow(new IOException()).when(harvesterService).harvest("repo1");
        HarvestRepositoryProcessor harvesterJob = new HarvestRepositoryProcessor(harvesterService, reposToHarvest);

        assertThrows(HarvestJobException.class, () -> harvesterJob.execute(mock(StepContribution.class), mock(ChunkContext.class)));

        verify(harvesterService, times(2)).harvest(any());
        verify(harvesterService).harvest("repo1");
        verify(harvesterService).harvest("repo2");
    }
}