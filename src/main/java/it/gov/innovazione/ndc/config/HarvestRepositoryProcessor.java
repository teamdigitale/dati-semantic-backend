package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterFinishedEvent;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterStartedEvent;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.HarvestJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;
import java.util.UUID;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static java.lang.Thread.currentThread;

@Slf4j
@RequiredArgsConstructor
public class HarvestRepositoryProcessor implements Tasklet, StepExecutionListener {
    private final HarvesterService harvesterService;
    private final RepositoryService repositoryService;
    private final HarvesterRunService harvesterRunService;
    private final NdcEventPublisher ndcEventPublisher;

    private Repository repository;
    private String correlationId;
    private String revision;
    private String principal;
    private ExitStatus exitStatus;

    // Used for Testing
    public HarvestRepositoryProcessor(
            HarvesterService harvesterService,
            NdcEventPublisher ndcEventPublisher,
            List<String> repository,
            RepositoryService repositoryService,
            HarvesterRunService harvesterRunService) {
        this.harvesterService = harvesterService;
        this.ndcEventPublisher = ndcEventPublisher;
        this.repository = asRepo(repository.get(0));
        this.exitStatus = ExitStatus.NOOP;
        this.repositoryService = repositoryService;
        this.harvesterRunService = harvesterRunService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters parameters = stepExecution.getJobExecution().getJobParameters();
        String repoId = parameters.getString("repository");
        correlationId = parameters.getString("correlationId");
        revision = parameters.getString("revision");
        principal = parameters.getString("principal");
        repository = repositoryService.findRepoById(repoId)
                .orElseThrow(() -> new HarvestJobException(String.format("Repository %s not found", repoId)));
        exitStatus = ExitStatus.NOOP;

        currentThread().setName("harvester-" + repository.getId());
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String runId = UUID.randomUUID().toString();
        try {
            publishHarvesterStartedEvent(repository, correlationId, revision, runId);
            verifyHarvestingIsNotInProgress(repository);

            harvesterService.harvest(repository, revision);

            publishHarvesterSuccessfulEvent(repository, correlationId, revision, runId);

            exitStatus = ExitStatus.COMPLETED;
        } catch (Exception e) {

            publishHarvesterFailedEvent(repository, correlationId, revision, runId, e);

            log.error("Unable to process {}", repository.getUrl(), e);
            exitStatus = ExitStatus.FAILED;
        }
        if (exitStatus == ExitStatus.FAILED) {
            throw new HarvestJobException(String.format("Harvesting failed for repos '%s'", repository.getUrl()));
        }

        return RepeatStatus.FINISHED;
    }

    private synchronized void verifyHarvestingIsNotInProgress(Repository repository) {
        if (harvesterRunService.isHarvestingInProgress(repository)) {
            throw new HarvestJobException(String.format("Harvesting for repo '%s' is already in progress", repository.getUrl()));
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        currentThread().setName("harvester");
        return exitStatus;
    }

    public void publishHarvesterStartedEvent(Repository repository, String correlationId, String revision, String runId) {
        ndcEventPublisher.publishEvent(
                "harvester",
                "harvester.started",
                correlationId,
                principal,
                HarvesterStartedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .build());
    }

    public void publishHarvesterSuccessfulEvent(Repository repository, String correlationId, String revision, String runId) {
        ndcEventPublisher.publishEvent(
                "harvester",
                "harvester.finished.success",
                correlationId,
                principal,
                HarvesterFinishedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .status(HarvesterRun.Status.SUCCESS)
                        .build());
    }

    public void publishHarvesterFailedEvent(Repository repository, String correlationId, String revision, String runId, Exception e) {
        ndcEventPublisher.publishEvent(
                "harvester",
                "harvester.finished.failure",
                correlationId,
                principal,
                HarvesterFinishedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .status(HarvesterRun.Status.FAILURE)
                        .exception(e)
                        .build());
    }
}
