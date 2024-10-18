package it.gov.innovazione.ndc.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.harvester.HarvesterJob;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.JobExecutionResponse;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@ConditionalOnProperty(name = "harvester.endpoint.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping
@RequiredArgsConstructor
public class HarvestJobController {
    private final HarvesterJob harvesterJob;
    private final HarvesterService harvesterService;
    private final HarvesterRunService harvesterRunService;
    private final NdcEventPublisher eventPublisher;

    @PostMapping("jobs/harvest")
    @Operation(
            operationId = "startHarvestJob",
            description = "Start a new harvest job",
            summary = "Start a new harvest job")
    public List<JobExecutionResponse> startHarvestJob(@RequestParam(required = false, defaultValue = "false") Boolean force) {
        log.info("Starting Harvest job at " + LocalDateTime.now());
        List<JobExecutionResponse> harvest = harvesterJob.harvest(force);
        eventPublisher.publishAlertableEvent(
                "Harvester",
                WebHarversterAlertableEvent.builder()
                        .description("Harvester execution started through REST API")
                        .context(Map.of("force", force))
                        .build());
        return harvest;
    }

    @GetMapping("jobs/harvest/run")
    @Operation(
            operationId = "getHarvestRuns",
            description = "Get all harvest runs",
            summary = "Get all harvest runs")
    public List<HarvesterRun> getAllRuns() {
        return harvesterRunService.getAllRuns();
    }

    @GetMapping("jobs/harvest/running")
    @Operation(
            operationId = "getRunningInstances",
            description = "Get all running instances",
            summary = "Get all running instances")
    public List<RunningInstance> getAllRunningInstance() {
        return harvesterRunService.getAllRunningInstances();
    }

    @DeleteMapping("jobs/harvest/run")
    @Operation(
            operationId = "deletePendingRuns",
            description = "Delete all pending runs",
            summary = "Delete all pending runs")
    public void deletePendingRuns() {
        harvesterRunService.deletePendingRuns();
        eventPublisher.publishAlertableEvent(
                "Harvester",
                WebHarversterAlertableEvent.builder()
                        .description("Harvester pending runs deleted")
                        .build());
    }

    @PostMapping(value = "jobs/harvest", params = "repositoryId")
    @Operation(
            operationId = "harvestRepositories",
            description = "Harvest a specific repository",
            summary = "Harvest a specific repository")
    public JobExecutionResponse harvestRepositories(
            @RequestParam("repositoryId") String repositoryId,
            @RequestParam(required = false, defaultValue = "") String revision,
            @RequestParam(required = false, defaultValue = "false") Boolean force) {
        log.info("Starting Harvest job at " + LocalDateTime.now() + "for repository " + repositoryId);
        JobExecutionResponse harvest = harvesterJob.harvest(repositoryId, revision, force);
        eventPublisher.publishAlertableEvent(
                "Harvester",
                WebHarversterAlertableEvent.builder()
                        .description("Harvester execution started through REST API")
                        .context(Map.of("repositoryId", repositoryId, "revision", revision, "force", force))
                        .build());
        return harvest;
    }

    @PostMapping("jobs/clear")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            operationId = "clearRepo",
            description = "Clear a repository",
            summary = "Clear a repository")
    public void clearRepo(@RequestParam("repo_url") String repoUrl) {
        if (StringUtils.isEmpty(repoUrl)) {
            throw new IllegalArgumentException("repo_url is required");
        }
        harvesterService.clear(repoUrl);
        eventPublisher.publishAlertableEvent(
                "Harvester",
                WebHarversterAlertableEvent.builder()
                        .description("Harvester repo cleared")
                        .context(Map.of("repo_url", repoUrl))
                        .build());
    }

    @Getter
    @Builder
    private static class WebHarversterAlertableEvent implements AlertableEvent {
        @Builder.Default
        private final String name = "Harvester Endpoint";
        private final String description;
        @Builder.Default
        private final Map<String, Object> context = Map.of();

        @Override
        public EventCategory getCategory() {
            return EventCategory.APPLICATION;
        }

        @Override
        public Severity getSeverity() {
            return Severity.INFO;
        }
    }

}
