package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.harvester.HarvesterService;
import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
public class ScheduledJobsController {

    private final HarvesterService harvesterService;

    @Autowired
    public ScheduledJobsController(HarvesterService harvesterService) {
        this.harvesterService = harvesterService;
    }

    @PostMapping("/harvester")
    public void csv(@RequestParam("repoURI") String repoUri) throws GitAPIException, IOException {
        harvesterService.harvest(repoUri);
    }
}
