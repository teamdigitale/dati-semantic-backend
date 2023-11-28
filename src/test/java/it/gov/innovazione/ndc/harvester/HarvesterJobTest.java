package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.util.GitUtils;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.util.List;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepos;
import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.FAILED;
import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.UNCHANGED;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HarvesterJobTest {
    @Mock
    JobLauncher jobLauncher;
    @Mock
    JobExplorer jobExplorer;
    @Mock
    Job harvestSemanticAssetsJob;
    @Mock
    RepositoryService repositoryService;
    @Mock
    GitUtils gitUtils;

    @InjectMocks
    HarvesterJob harvesterJob;
    String repositories = "repo1";

    @Test
    void shouldLaunchHarvestJobWithTodaysDate() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

        List<Repository> repos = asRepos(repositories);
        when(repositoryService.getAllRepos()).thenReturn(repos);
        when(jobLauncher.run(any(), any())).thenReturn(mock(JobExecution.class));

        harvesterJob.harvest();

        ArgumentCaptor<JobParameters> paramCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(harvestSemanticAssetsJob), paramCaptor.capture());

        JobParameters actualJobParams = paramCaptor.getValue();
        assertEquals(repos.get(0).getId(), actualJobParams.getString("repository"));
    }

    @Test
    void shouldSaveHarvesterRunWhenJobThrowsGenericException()
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

        when(repositoryService.getAllRepos()).thenReturn(asRepos(repositories));

        doThrow(new RuntimeException()).when(jobLauncher).run(any(), any());
        harvesterJob.harvest();
        ArgumentCaptor<HarvesterRun> harvesterRunCaptor = ArgumentCaptor.forClass(HarvesterRun.class);
        verify(repositoryService).saveHarvesterRun(harvesterRunCaptor.capture());
        assertEquals(FAILED, harvesterRunCaptor.getValue().getStatus());
    }

    @Test
    void shouldSaveUnchangedHarvesterRunWhenJobThrowsJobInstanceAlreadyCompleteException()
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

        when(repositoryService.getAllRepos()).thenReturn(asRepos(repositories));

        doThrow(mock(JobInstanceAlreadyCompleteException.class)).when(jobLauncher).run(any(), any());
        harvesterJob.harvest();
        ArgumentCaptor<HarvesterRun> harvesterRunCaptor = ArgumentCaptor.forClass(HarvesterRun.class);
        verify(repositoryService).saveHarvesterRun(harvesterRunCaptor.capture());
        assertEquals(UNCHANGED, harvesterRunCaptor.getValue().getStatus());
    }

    @Test
    void shouldGetStatusForLatestHarvestingJob() {
        JobInstance mockJobInstance = mock(JobInstance.class);
        JobExecution jobExecution = new JobExecution(123L);
        when(jobExplorer.getLastJobInstance("harvestSemanticAssetsJob")).thenReturn(mockJobInstance);
        when(jobExplorer.getJobExecutions(mockJobInstance)).thenReturn(asList(jobExecution));

        List<JobExecutionStatusDto> statuses = harvesterJob.getStatusOfLatestHarvestingJob();
        assertEquals(statuses.size(), 1);
    }

    @Test
    void shouldGetStatusForHarvestingJobs() {
        JobInstance mockJobInstance = mock(JobInstance.class);
        JobExecution jobExecution = new JobExecution(123L);
        when(jobExplorer.getJobInstances("harvestSemanticAssetsJob", 0, 30)).thenReturn(asList(mockJobInstance));
        when(jobExplorer.getJobExecutions(mockJobInstance)).thenReturn(asList(jobExecution));

        List<JobExecutionStatusDto> statuses = harvesterJob.getStatusOfHarvestingJobs();
        assertEquals(statuses.size(), 1);
    }

}
