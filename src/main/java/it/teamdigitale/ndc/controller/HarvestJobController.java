package it.teamdigitale.ndc.controller;

import io.swagger.v3.oas.annotations.Hidden;
import it.teamdigitale.ndc.harvester.HarvesterJob;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Hidden
@RestController
@RequestMapping
public class HarvestJobController {
    private final HarvesterService harvesterService;
    private final HarvesterJob harvesterJob;

    @Autowired
    public HarvestJobController(HarvesterService harvesterService, HarvesterJob harvesterJob) {
        this.harvesterService = harvesterService;
        this.harvesterJob = harvesterJob;
    }

    @PostMapping("harvest/start")
    public void startHarvestJob() {
        log.info("Starting Harvest job at " + LocalDateTime.now());
        harvesterJob.harvest();
    }

    @PostMapping("scheduler/harvester")
    public void csv(@RequestParam("repoURI") String repoUri) throws IOException {
        harvesterService.harvest(repoUri);
    }
}
