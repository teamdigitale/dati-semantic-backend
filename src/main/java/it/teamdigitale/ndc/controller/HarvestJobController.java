package it.teamdigitale.ndc.controller;

import io.swagger.v3.oas.annotations.Hidden;
import it.teamdigitale.ndc.harvester.HarvesterJob;
import it.teamdigitale.ndc.harvester.JobExecutionStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Hidden
@RestController
@RequestMapping
public class HarvestJobController {
    private final HarvesterJob harvesterJob;

    @Autowired
    public HarvestJobController(HarvesterJob harvesterJob) {
        this.harvesterJob = harvesterJob;
    }

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
}
