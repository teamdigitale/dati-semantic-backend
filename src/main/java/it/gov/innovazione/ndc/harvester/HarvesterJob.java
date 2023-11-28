package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.util.GitUtils;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final GitUtils gitUtils;

    public void harvest(Boolean force) {
        List<Repository> allRepos = repositoryService.getAllRepos();
        harvest(allRepos, force);
    }

    public void harvest(List<Repository> repositories, Boolean force) {
        String correlationId = UUID.randomUUID().toString();
        for (Repository repository : repositories) {
            harvest(repository, correlationId, force);
        }
    }

    public void harvest(List<Repository> repos) {
        harvest(repos, false);
    }

    public void harvest() {
        harvest(false);
    }

    private void harvest(Repository repository, String correlationId, boolean force) {
        try {
            String revision = gitUtils.getCurrentRemoteRevision(repository.getUrl());

            String uniquifier = force ? UUID.randomUUID().toString() : "";

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("uniquifier", uniquifier)
                    .addString("revision", revision)
                    .addString("repository", repository.getId())
                    .addString("correlationId", correlationId, false)
                    .toJobParameters();

            JobExecution run = jobLauncher.run(harvestSemanticAssetsJob, jobParameters);
            log.info("Harvest job started at " + LocalDateTime.now());
            Optional.of(run)
                    .map(JobExecution::getJobInstance)
                    .ifPresent(jobInstance -> log.info("Harvester job instance:" + jobInstance));
        } catch (JobInstanceAlreadyCompleteException e) {
            repositoryService.saveHarvesterRun(
                    HarvesterRun.builder()
                            .id(UUID.randomUUID().toString())
                            .correlationId(correlationId)
                            .repositoryId(repository.getId())
                            .repositoryUrl(repository.getUrl())
                            .startedAt(Instant.now())
                            .startedBy("harvester")
                            .endedAt(Instant.now())
                            .revision(gitUtils.getCurrentRemoteRevision(repository.getUrl()))
                            .status(HarvesterRun.Status.UNCHANGED)
                            .build());
        } catch (Exception e) {
            repositoryService.saveHarvesterRun(
                    HarvesterRun.builder()
                            .id(UUID.randomUUID().toString())
                            .correlationId(correlationId)
                            .repositoryId(repository.getId())
                            .repositoryUrl(repository.getUrl())
                            .startedAt(Instant.now())
                            .startedBy("harvester")
                            .endedAt(Instant.now())
                            .revision(gitUtils.getCurrentRemoteRevision(repository.getUrl()))
                            .status(HarvesterRun.Status.FAILED)
                            .reason(e.getMessage())
                            .build());
        }
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
