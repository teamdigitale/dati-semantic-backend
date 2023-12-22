package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.HarvesterJob;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.JobExecutionResponse;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@ConditionalOnProperty(name = "harvester.endpoint.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping
@RequiredArgsConstructor
public class HarvestJobController {
    private final HarvesterJob harvesterJob;
    private final HarvesterService harvesterService;
    private final HarvesterRunService harvesterRunService;

    @PostMapping("jobs/harvest")
    public List<JobExecutionResponse> startHarvestJob(@RequestParam(required = false, defaultValue = "false") Boolean force) {
        log.info("Starting Harvest job at " + LocalDateTime.now());
        return harvesterJob.harvest(force);
    }

    @GetMapping("jobs/harvest/run")
    public List<HarvesterRun> getAllRuns() {
        return harvesterRunService.getAllRuns();
    }

    @GetMapping("jobs/harvest/running")
    public List<RunningInstance> getAllRunningInstance() {
        return harvesterRunService.getAllRunningInstances();
    }

    @DeleteMapping("jobs/harvest/run")
    public void deletePendingRuns() {
        harvesterRunService.deletePendingRuns();
    }

    @PostMapping(value = "jobs/harvest", params = "repositoryId")
    public JobExecutionResponse harvestRepositories(
            @RequestParam("repositoryId") String repositoryId,
            @RequestParam(required = false) String revision,
            @RequestParam(required = false, defaultValue = "false") Boolean force) {
        log.info("Starting Harvest job at " + LocalDateTime.now() + "for repository " + repositoryId);
        return harvesterJob.harvest(repositoryId, revision, force);
    }

    @PostMapping("jobs/clear")
    public void clearRepo(@RequestParam("repo_url") String repoUrl) {
        harvesterService.clear(repoUrl);
    }

}
