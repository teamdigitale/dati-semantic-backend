package it.gov.innovazione.ndc.harvester.service.startupjob;

import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.util.GitUtils;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class UpdateRevisionCommittedAt implements StartupJob {

    private final GitUtils gitUtils;
    private final HarvesterRunService harvesterRunService;

    @Override
    public void run() {
        List<HarvesterRun> harvesterRunsToUpdate = harvesterRunService.getAllRuns().stream()
                .filter(this::hasNoRevisionCommittedAt)
                .filter(this::isSuccess)
                .toList();
        NDCHarvesterLogger.logApplicationInfo(
                LoggingContext.builder()
                        .component("UpdateRevisionCommittedAt")
                        .message(String.format("Found %d harvester runs to update", harvesterRunsToUpdate.size()))
                        .build());
    Map<Pair<String, String>, List<HarvesterRun>> byRepositoryAndRevision =
        harvesterRunsToUpdate.stream()
            .filter(this::hasNoRevisionCommittedAt)
            .filter(this::isSuccess)
            .collect(
                Collectors.groupingBy(
                    harvesterRun ->
                        Pair.of(harvesterRun.getRepositoryUrl(), harvesterRun.getRevision())));

        byRepositoryAndRevision.entrySet().stream()
                .map(this::withUpdatedRevisionCommittedAt)
                .flatMap(List::stream)
                .forEach(this::logAndUpdate);

    }

    private boolean hasNoRevisionCommittedAt(HarvesterRun harvesterRun) {
        return isNull(harvesterRun.getRevisionCommittedAt());
    }

    private boolean isSuccess(HarvesterRun harvesterRun) {
    return harvesterRun.getStatus() == HarvesterRun.Status.SUCCESS;
  }

    @Scheduled(cron = "${ndc.harvester.update-revision-committed-at.cron}")
    public void scheduledRun() {
        NDCHarvesterLogger.logApplicationInfo(
                LoggingContext.builder()
                        .component("UpdateRevisionCommittedAt")
                        .message("Scheduled run of UpdateRevisionCommittedAt")
                        .build());
        run();
    }

    private void logAndUpdate(HarvesterRun harvesterRun) {
        Instant revisionCommittedAt = harvesterRun.getRevisionCommittedAt();
        String repositoryUrl = harvesterRun.getRepositoryUrl();
        String revision = harvesterRun.getRevision();
        String harvesterRunId = harvesterRun.getId();
        NDCHarvesterLogger.logApplicationInfo(
                LoggingContext.builder()
                        .message(String.format("Updated revisionCommittedAt for harvester run %s with revision %s and repository %s to %s",
                                harvesterRunId, revision, repositoryUrl, revisionCommittedAt))
                        .build());
        harvesterRunService.updateHarvesterRunCommittedAt(harvesterRun);
    }

    private List<HarvesterRun> withUpdatedRevisionCommittedAt(Map.Entry<Pair<String, String>, List<HarvesterRun>> byRepositoryAndRevision) {
        Pair<String, String> repositoryAndRevision = byRepositoryAndRevision.getKey();
        Optional<Instant> commitDate = gitUtils.getCommitDate(repositoryAndRevision.getLeft(), repositoryAndRevision.getRight());
        return commitDate.map(instant -> byRepositoryAndRevision.getValue().stream()
                .map(harvesterRun -> harvesterRun.withRevisionCommittedAt(instant))
                .toList()).orElseGet(byRepositoryAndRevision::getValue);
    }
}

