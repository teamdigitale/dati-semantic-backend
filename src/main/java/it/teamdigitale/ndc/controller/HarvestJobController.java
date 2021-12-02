package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.harvester.HarvesterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping
public class HarvestJobController {
    private final HarvesterService harvesterService;
    private final JobLauncher jobLauncher;
    private final Job harvestSemanticAssetsJob;

    @Autowired
    public HarvestJobController(HarvesterService harvesterService, JobLauncher jobLauncher, Job harvestSemanticAssetsJob) {
        this.harvesterService = harvesterService;
        this.jobLauncher = jobLauncher;
        this.harvestSemanticAssetsJob = harvestSemanticAssetsJob;
    }

    @PostMapping("harvest/start")
    public String startHarvestJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()).toJobParameters();
            jobLauncher.run(harvestSemanticAssetsJob, jobParameters);
        } catch (Exception e) {
            log.error(e.getMessage());
            return "Harvest job has error " + e.getMessage();
        }
        return "Harvest job started";
    }

    @PostMapping("scheduler/harvester")
    public void csv(@RequestParam("repoURI") String repoUri) throws IOException {
        harvesterService.harvest(repoUri);
    }
}
