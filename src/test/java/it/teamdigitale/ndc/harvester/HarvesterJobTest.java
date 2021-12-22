package it.teamdigitale.ndc.harvester;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HarvesterJobTest {
    @Mock
    JobLauncher jobLauncher;
    @Mock
    JobExplorer jobExplorer;
    @Mock
    Job harvestSemanticAssetsJob;
    @Mock
    Clock clock;

    @InjectMocks
    HarvesterJob harvesterJob;
    String repositories = "repo1,repo2";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(harvesterJob, "repositories", repositories);
    }

    @Test
    void shouldLaunchHarvestJobWithTodaysDate() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        setupClock();

        harvesterJob.harvest();

        ArgumentCaptor<JobParameters> paramCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(harvestSemanticAssetsJob), paramCaptor.capture());

        JobParameters actualJobParams = paramCaptor.getValue();
        assertEquals("2021-12-01 00:00", actualJobParams.getString("harvestTime"));
        assertEquals(repositories, actualJobParams.getString("repositories"));
    }

    @Test
    void shouldFailGracefullyWhenJobThrowsException() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        setupClock();

        doThrow(new RuntimeException()).when(jobLauncher).run(any(), any());
        harvesterJob.harvest();
    }

    @Test
    void shouldLaunchHarvestJobWithSpecifiedRepositories() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        setupClock();

        String userGivenRepos = "repo3,repo4";
        harvesterJob.harvest(userGivenRepos);

        ArgumentCaptor<JobParameters> paramCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(harvestSemanticAssetsJob), paramCaptor.capture());

        JobParameters actualJobParams = paramCaptor.getValue();
        assertEquals("2021-12-01 00:00", actualJobParams.getString("harvestTime"));
        assertEquals(userGivenRepos, actualJobParams.getString("repositories"));
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

    private void setupClock() {
        Clock fixedClock = Clock.fixed(LocalDate.of(2021, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
    }
}