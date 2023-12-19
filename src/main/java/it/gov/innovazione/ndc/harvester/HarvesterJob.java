package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.util.GitUtils;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.HarvestJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HarvesterJob {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job harvestSemanticAssetsJob;
    private final RepositoryService repositoryService;
    private final HarvesterRunService harvesterRunService;
    private final NdcEventPublisher ndcEventPublisher;
    private final GitUtils gitUtils;

    public List<JobExecutionResponse> harvest(Boolean force) {
        List<Repository> allRepos = repositoryService.getAllRepos();
        return harvest(allRepos, force);
    }

    public List<JobExecutionResponse> harvest(List<Repository> repositories, Boolean force) {
        String correlationId = UUID.randomUUID().toString();
        List<JobExecutionResponse> responses = new ArrayList<>();
        for (Repository repository : repositories) {
            responses.add(harvest(repository, correlationId, force));
        }
        return responses;
    }

    public List<JobExecutionResponse> harvest(List<Repository> repos) {
        return harvest(repos, false);
    }

    public List<JobExecutionResponse> harvest() {
        return harvest(false);
    }

    private JobExecutionResponse harvest(Repository repository, String correlationId, boolean force) {
        return harvest(repository, correlationId, null, force);
    }

    public JobExecutionResponse harvest(String repositoryId, String revision, Boolean force) {
        Repository repository = repositoryService.findRepoById(repositoryId)
                .orElseThrow(() -> new HarvestJobException(String.format("Repository %s not found", repositoryId)));
        String correlationId = UUID.randomUUID().toString();
        return harvest(repository, correlationId, revision, force);
    }


    private JobExecutionResponse harvest(Repository repository, String correlationId, String revision, boolean force) {
        JobExecutionResponse.JobExecutionResponseBuilder responseBuilder = JobExecutionResponse.builder()
                .correlationId(correlationId)
                .repositoryId(repository.getId())
                .repositoryUrl(repository.getUrl())
                .startedAt(Instant.now().toString())
                .status(JobExecutionResponse.ExecutionStatus.STARTED)
                .forced(force);
        try {
            revision = Optional.ofNullable(revision)
                    .filter(StringUtils::isNotBlank)
                    .orElseGet(() -> gitUtils.getCurrentRemoteRevision(repository.getUrl()));

            String uniquifier = force ? UUID.randomUUID().toString() : "";

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("uniquifier", uniquifier)
                    .addString("revision", revision)
                    .addString("repository", repository.getId())
                    .addString("correlationId", correlationId, false)
                    .addString("principal", SecurityUtils.getCurrentUserLogin(), false)
                    .toJobParameters();

            JobExecution run = jobLauncher.run(harvestSemanticAssetsJob, jobParameters);

            JobExecutionResponse response = responseBuilder
                    .jobId(String.valueOf(run.getJobId()))
                    .jobInstanceId(String.valueOf(run.getJobInstance().getId()))
                    .jobParameters(run.getJobParameters().getParameters())
                    .build();

            log.info("Harvest job started at " + LocalDateTime.now());
            Optional.of(run)
                    .map(JobExecution::getJobInstance)
                    .ifPresent(jobInstance -> log.info("Harvester job instance:" + jobInstance));

            return response;
        } catch (JobInstanceAlreadyCompleteException e) {
            harvesterRunService.saveHarvesterRun(
                    HarvesterRun.builder()
                            .id(UUID.randomUUID().toString())
                            .correlationId(correlationId)
                            .repositoryId(repository.getId())
                            .repositoryUrl(repository.getUrl())
                            .startedAt(Instant.now())
                            .startedBy(SecurityUtils.getCurrentUserLogin())
                            .endedAt(Instant.now())
                            .revision(gitUtils.getCurrentRemoteRevision(repository.getUrl()))
                            .status(HarvesterRun.Status.UNCHANGED)
                            .build());

            responseBuilder = responseBuilder
                    .status(JobExecutionResponse.ExecutionStatus.UNCHANGED);

        } catch (Exception e) {
            harvesterRunService.saveHarvesterRun(
                    HarvesterRun.builder()
                            .id(UUID.randomUUID().toString())
                            .correlationId(correlationId)
                            .repositoryId(repository.getId())
                            .repositoryUrl(repository.getUrl())
                            .startedAt(Instant.now())
                            .startedBy(SecurityUtils.getCurrentUserLogin())
                            .endedAt(Instant.now())
                            .revision(gitUtils.getCurrentRemoteRevision(repository.getUrl()))
                            .status(HarvesterRun.Status.FAILURE)
                            .reason(e.getMessage())
                            .build());

            responseBuilder = responseBuilder.status(JobExecutionResponse.ExecutionStatus.FAILURE);
        }
        return responseBuilder.build();
    }

    public List<JobExecutionStatusDto> getStatusOfLatestHarvestingJob() {
        JobInstance latestJobInstance = jobExplorer.getLastJobInstance("harvestSemanticAssetsJob");
        if (isNull(latestJobInstance)) {
            return emptyList();
        }

        return jobExplorer.getJobExecutions(latestJobInstance).stream().map(JobExecutionStatusDto::new).collect(toList());
    }

    public List<JobExecutionStatusDto> getStatusOfHarvestingJobs() {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("harvestSemanticAssetsJob", 0, 30);
        if (jobInstances.isEmpty()) {
            return emptyList();
        }

        List<JobExecutionStatusDto> statuses = new ArrayList<>();
        for (JobInstance jobInstance : jobInstances) {
            statuses.addAll(jobExplorer.getJobExecutions(jobInstance).stream().map(JobExecutionStatusDto::new).collect(toList()));
        }
        return statuses;
    }
}
