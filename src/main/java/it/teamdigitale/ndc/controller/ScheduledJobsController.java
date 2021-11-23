package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.harvester.HarvesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/scheduler")
public class ScheduledJobsController {

    private final HarvesterService harvesterService;

    @Autowired
    public ScheduledJobsController(HarvesterService harvesterService) {
        this.harvesterService = harvesterService;
    }

    @PostMapping("/harvester")
    public void csv(@RequestParam("repoURI") String repoUri) throws IOException {
        harvesterService.harvest(repoUri);
    }
}
