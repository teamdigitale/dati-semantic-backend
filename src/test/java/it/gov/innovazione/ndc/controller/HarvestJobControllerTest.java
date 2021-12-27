package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.HarvesterJob;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.JobExecutionStatusDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HarvestJobControllerTest {

    @Mock
    HarvesterJob harvesterJob;
    @Mock
    HarvesterService harvesterService;
    @InjectMocks
    HarvestJobController harvestJobController;

    @Test
    void shouldStartHarvestJobForAllRepositories() {
        harvestJobController.startHarvestJob();
        verify(harvesterJob).harvest();
    }

    @Test
    void shouldGetStatusForLatestHarvestedJob() {
        List<JobExecutionStatusDto> expected = asList(mock(JobExecutionStatusDto.class));
        when(harvesterJob.getStatusOfLatestHarvestingJob()).thenReturn(expected);
        List<JobExecutionStatusDto> latestStatus = harvestJobController.getStatusOfLatestHarvestingJob();

        assertEquals(expected, latestStatus);
    }

    @Test
    void shouldGetStatusForHarvestJobs() {
        List<JobExecutionStatusDto> expected = asList(mock(JobExecutionStatusDto.class));
        when(harvesterJob.getStatusOfHarvestingJobs()).thenReturn(expected);
        List<JobExecutionStatusDto> latestStatus = harvestJobController.getStatusOfHarvestingJobs();

        assertEquals(expected, latestStatus);
    }

    @Test
    void shouldStartHarvestForSpecifiedRepositories() throws IOException {
        String repoUrls = "http://github.com/repo,http://github.com/repo2";
        harvestJobController.harvestRepositories(repoUrls);
        verify(harvesterJob).harvest(repoUrls);
    }

    @Test
    void shouldSynchronouslyClearRepo() {
        String repoUrl = "http://github.com/repo.git";
        harvestJobController.clearRepo(repoUrl);
        verify(harvesterService).clear(repoUrl);
    }
}