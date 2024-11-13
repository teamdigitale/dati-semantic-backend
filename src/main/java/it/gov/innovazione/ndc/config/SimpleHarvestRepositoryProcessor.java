package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterFinishedEvent;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterStartedEvent;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.exception.HarvesterAlreadyExecutedException;
import it.gov.innovazione.ndc.harvester.exception.HarvesterAlreadyInProgressException;
import it.gov.innovazione.ndc.harvester.exception.HarvesterException;
import it.gov.innovazione.ndc.harvester.exception.RepoContainsNdcIssueException;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.service.GithubService;
import it.gov.innovazione.ndc.service.InstanceManager;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLoggerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ThreadUtils;
import org.kohsuke.github.GHIssue;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.config.AsyncConfiguration.THREAD_PREFIX;
import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.ALREADY_RUNNING;
import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.FAILURE;
import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.RUNNING;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticWarn;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.startsWith;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class SimpleHarvestRepositoryProcessor {

    private final HarvesterService harvesterService;
    private final HarvesterRunService harvesterRunService;
    private final NdcEventPublisher ndcEventPublisher;
    private final GithubService githubService;

    private final List<String> locks = new ArrayList<>();
    private final InstanceManager instanceManager;

    public static List<String> getAllRunningHarvestThreadNames() {
        return ThreadUtils.getAllThreads().stream()
                .map(Thread::getName)
                .filter(name -> startsWith(name, THREAD_PREFIX))
                .filter(name -> endsWith(name, "RUNNING"))
                .collect(Collectors.toList());
    }

    private void setThreadName(String runId, String repoId, String revision, String status) {
        Thread.currentThread().setName(THREAD_PREFIX + "|" + runId + "|" + repoId + "|" + revision + "|" + status);
    }

    @Async
    public void execute(String runId, Repository repository, String correlationId, String revision, boolean force, String currentUserLogin) {
        try {
            setThreadName(runId, repository.getId(), revision, "RUNNING");

            Instance instanceToHarvest = instanceManager.getNextOnlineInstance(repository);

            HarvestExecutionContextUtils.setContext(
                    HarvestExecutionContext.builder()
                            .repository(repository)
                            .revision(revision)
                            .correlationId(correlationId)
                            .runId(runId)
                            .currentUserId(currentUserLogin)
                            .instance(instanceToHarvest)
                            .build());

            NDCHarvesterLoggerUtils.setInitialContext(
                    LoggingContext.builder()
                            .jobId(runId)
                            .repoUrl(repository.getUrl())
                            .stage(HarvesterStage.START)
                            .harvesterStatus(RUNNING)
                            .component("HARVESTER")
                            .additionalInfo("revision", revision)
                            .additionalInfo("correlationId", correlationId)
                            .additionalInfo("currentUserLogin", currentUserLogin)
                            .additionalInfo("instance", instanceToHarvest)
                            .additionalInfo("force", force)
                            .build());

            publishHarvesterStartedEvent(repository, correlationId, revision, runId, currentUserLogin, instanceToHarvest);

            logSemanticInfo(LoggingContext.builder()
                    .message("Starting harvester on repo " + repository.getUrl())
                    .build());

            synchronized (locks) {
                if (locks.contains(repository.getId() + revision)) {
                    log.info("Harvesting for repo '{}' is already in progress", repository.getUrl());
                    publishHarvesterFailedEvent(
                            repository,
                            correlationId,
                            revision,
                            runId,
                            ALREADY_RUNNING,
                            new HarvesterAlreadyInProgressException(
                                    format("Harvesting for repo %s is already running",
                                            repository.getUrl())),
                            currentUserLogin);

                    logSemanticError(LoggingContext.builder()
                            .harvesterStatus(ALREADY_RUNNING)
                            .message("Failed harvester on repo " + repository.getUrl())
                            .details("Harvester on repo " + repository.getUrl() + " is already running")
                            .build());

                    setThreadName(runId, repository.getId(), revision, "IDLE");
                    return;
                }
                locks.add(repository.getId() + revision);
            }

            verifyHarvestingIsNotInProgress(runId, repository);

            if (!force) {
                verifySameRunWasNotExecuted(repository, revision);
            }

            verifyNoNdcIssuesInRepoIfNecessary(repository);

            harvesterService.harvest(repository, revision, instanceToHarvest);

            githubService.openIssueIfNecessary();

            publishHarvesterSuccessfulEvent(repository, correlationId, revision, runId, currentUserLogin);

            instanceManager.switchInstances(repository);

            setThreadName(runId, repository.getId(), revision, "IDLE");
        } catch (HarvesterException e) {
            publishHarvesterFailedEvent(repository, correlationId, revision, runId, e.getHarvesterRunStatus(), e, currentUserLogin);
        } catch (Exception e) { // other exceptions
            publishHarvesterFailedEvent(repository, correlationId, revision, runId, FAILURE, e, currentUserLogin);
            log.error("Unable to process {}", repository.getUrl(), e);
        } finally {
            log.info("Cleaning up after processing {}", repository.getUrl());
            harvesterService.clearTempGraphIfExists(repository.getUrl());
            NDCHarvesterLoggerUtils.clearContext();
            log.info("Cleaned up after processing {}", repository.getUrl());

        }

        // cleanup
        removeLock(repository, revision);
        setThreadName(runId, repository.getId(), revision, "IDLE");

        // clean the context
        HarvestExecutionContextUtils.clearContext();
    }

    private void verifyNoNdcIssuesInRepoIfNecessary(Repository repository) {
        Optional<GHIssue> ndcIssue = githubService.getNdcIssueIfPresent(repository.getUrl());
        boolean hasIssues = ndcIssue.isPresent();
        if (hasIssues) {
            URL issueUrl = ndcIssue.get().getUrl();
            logSemanticError(LoggingContext.builder()
                    .harvesterStatus(FAILURE)
                    .repoUrl(repository.getUrl())
                    .message("Repository has at least one open NDC issues")
                    .additionalInfo("issueUrl", issueUrl)
                    .build());
            throw new RepoContainsNdcIssueException(format("Repository %s has NDC issues [%s]",
                    repository.getUrl(),
                    issueUrl.toString()));
        }
    }

    private void removeLock(Repository repository, String revision) {
        synchronized (locks) {
            locks.remove(repository.getId() + revision);
        }
    }

    private synchronized void verifySameRunWasNotExecuted(Repository repository, String revision) {
        Optional<HarvesterRun> harvesterRun = harvesterRunService.isHarvestingAlreadyExecuted(repository.getId(), revision);
        if (harvesterRun.isPresent()) {
            logSemanticWarn(LoggingContext.builder()
                    .harvesterStatus(HarvesterRun.Status.UNCHANGED)
                    .message("Harvesting for repo '" + repository.getUrl() + "' was already executed")
                    .additionalInfo("otherJobId", harvesterRun.get().getId())
                    .build());
            throw new HarvesterAlreadyExecutedException(format("Harvesting for repo '%s' with revision '%s' was already executed and no force param was passed",
                    repository.getUrl(), revision));
        }
    }

    private synchronized void verifyHarvestingIsNotInProgress(String runId, Repository repository) {
        Optional<HarvesterRun> harvestingInProgress = harvesterRunService.isHarvestingInProgress(runId, repository);
        if (harvestingInProgress.isPresent()) {
            logSemanticError(LoggingContext.builder()
                    .message("Harvesting for repo '" + repository.getUrl() + "' is already in progress")
                    .harvesterStatus(ALREADY_RUNNING)
                    .additionalInfo("otherJobId", harvestingInProgress.get().getId())
                    .build());
            throw new HarvesterAlreadyInProgressException(format("Harvesting for repo '%s' is already in progress", repository.getUrl()));
        }
    }

    public void publishHarvesterStartedEvent(Repository repository, String correlationId, String revision, String runId, String currentUserLogin, Instance instance) {
        ndcEventPublisher.publishEvent(
                "harvester",
                "harvester.started",
                correlationId,
                currentUserLogin,
                HarvesterStartedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .instance(instance)
                        .revision(revision)
                        .build());
    }

    public void publishHarvesterSuccessfulEvent(Repository repository, String correlationId, String revision, String runId, String currentUserLogin) {
        ndcEventPublisher.publishEvent(
                "harvester",
                "harvester.finished.success",
                correlationId,
                currentUserLogin,
                HarvesterFinishedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .status(HarvesterRun.Status.SUCCESS)
                        .build());
    }

    public void publishHarvesterFailedEvent(
            Repository repository,
            String correlationId,
            String revision,
            String runId,
            HarvesterRun.Status status,
            Exception e, String currentUserLogin) {
        ndcEventPublisher.publishEvent(
                "harvester",
                "harvester.finished.failure",
                correlationId,
                currentUserLogin,
                HarvesterFinishedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .status(status)
                        .exception(e)
                        .build());
    }
}
