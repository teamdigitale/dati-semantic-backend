package it.teamdigitale.ndc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class HarvestJobController {
    final JobLauncher jobLauncher;
    final Job harvestSemanticAssetsJob;

    @GetMapping("harvest/start")
    public String startHarvestJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()).toJobParameters();
            jobLauncher.run(harvestSemanticAssetsJob, jobParameters);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return "Harvest job started";
    }
}
