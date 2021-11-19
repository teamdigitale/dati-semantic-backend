package it.teamdigitale.ndc.harvester;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

class HarvesterJobTest {

    @Test
    void shouldHarvestAllRepos() throws GitAPIException, IOException {
        HarvesterService harvesterService = mock(HarvesterService.class);
        List<String> reposToHarvest = List.of("repo1", "repo2");
        HarvesterJob harvesterJob = new HarvesterJob(harvesterService, reposToHarvest);

        harvesterJob.harvest();

        verify(harvesterService, times(2)).harvest(any());
        verify(harvesterService).harvest("repo1");
        verify(harvesterService).harvest("repo2");
    }

    @Test
    void shouldHarvestAllReposContinueToNextInCaseOfFailure() throws GitAPIException, IOException {
        HarvesterService harvesterService = mock(HarvesterService.class);
        List<String> reposToHarvest = List.of("repo1", "repo2");
        doThrow(new IOException()).when(harvesterService).harvest("repo1");
        HarvesterJob harvesterJob = new HarvesterJob(harvesterService, reposToHarvest);

        harvesterJob.harvest();

        verify(harvesterService, times(2)).harvest(any());
        verify(harvesterService).harvest("repo1");
        verify(harvesterService).harvest("repo2");
    }
}