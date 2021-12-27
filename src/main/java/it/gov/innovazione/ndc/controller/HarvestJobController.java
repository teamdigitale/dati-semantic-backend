package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.HarvesterJob;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.JobExecutionStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@ConditionalOnProperty(name = "harvester.endpoint.enabled", havingValue = "true")
@RequestMapping
@RequiredArgsConstructor
public class HarvestJobController {
    private final HarvesterJob harvesterJob;
    private final HarvesterService harvesterService;

    @PostMapping("jobs/harvest")
    public void startHarvestJob() {
        log.info("Starting Harvest job at " + LocalDateTime.now());
        harvesterJob.harvest();
    }

    @GetMapping("jobs/harvest/latest")
    public List<JobExecutionStatusDto> getStatusOfLatestHarvestingJob() {
        return harvesterJob.getStatusOfLatestHarvestingJob();
    }

    @GetMapping("jobs/harvest")
    public List<JobExecutionStatusDto> getStatusOfHarvestingJobs() {
        return harvesterJob.getStatusOfHarvestingJobs();
    }

    @PostMapping("jobs/harvest/repositories")
    public void harvestRepositories(@RequestParam("repo_urls") String repoUrl) {
        harvesterJob.harvest(repoUrl);
    }

    @PostMapping("jobs/clear/")
    public void clearRepo(@RequestParam("repo_url") String repoUrl) {
        harvesterService.clear(repoUrl);
    }
}
