package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.HarvesterJob;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.JobExecutionResponse;
import it.gov.innovazione.ndc.harvester.JobExecutionStatusDto;
import it.gov.innovazione.ndc.harvester.service.RepositoryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@ConditionalOnProperty(name = "harvester.endpoint.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping
@RequiredArgsConstructor
public class HarvestJobController {
    private final HarvesterJob harvesterJob;
    private final HarvesterService harvesterService;

    /*
     * POST config/repository/
     * <p>
     * {
     * "url": "",
     * "name": "",
     * "description": ""
     * }
     * <p>
     * return 201 CREATED config/repository/{id}
     * <p>
     * DELETE config/repository/{id}
     * return 204 NO CONTENT
     * <p>
     * PATCH config/repository/{id}
     * {
     * "url": "",
     * "name": "",
     * "description": ""
     * }
     */

    @PostMapping("jobs/harvest")
    public List<JobExecutionResponse> startHarvestJob(@RequestParam(required = false, defaultValue = "false") Boolean force) {
        log.info("Starting Harvest job at " + LocalDateTime.now());
        return harvesterJob.harvest(force);
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
        harvesterJob.harvest(RepositoryUtils.asRepos(repoUrl));
    }

    @PostMapping("jobs/clear")
    public void clearRepo(@RequestParam("repo_url") String repoUrl) {
        harvesterService.clear(repoUrl);
    }

}
