package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.service.SemanticContentStatsService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.SUCCESS;
import static java.util.stream.Collectors.toMap;

@Component
@RequiredArgsConstructor
public class DashboardRepo {

    private final RepositoryService repositoryService;
    private final HarvesterRunService harvesterRunService;
    private final SemanticContentStatsService semanticContentStatsService;

    private List<HarvesterRun> allRuns;
    private Map<String, HarvesterRun> runById;
    private List<SemanticContentStats> allStats;
    private Map<String, Repository> repoById;

    public synchronized List<HarvesterRun> getAllRuns() {
        if (allRuns == null) {
            allRuns = harvesterRunService.getAllRuns();
        }
        return allRuns;
    }

    public synchronized Map<String, HarvesterRun> getRunById() {
        if (runById == null) {
            runById = getAllRuns().stream()
                    .filter(run -> SUCCESS.equals(run.getStatus()))
                    .collect(toMap(HarvesterRun::getId, Function.identity()));
        }
        return runById;
    }

    public synchronized List<SemanticContentStats> getAllStats() {
        if (allStats == null) {
            allStats = semanticContentStatsService.getRawStats();
        }
        return allStats;
    }

    public synchronized Map<String, Repository> getRepoById() {
        if (repoById == null) {
            repoById = repositoryService.getActiveRepos()
                    .stream()
                    .collect(toMap(Repository::getId, Function.identity()));
        }
        return repoById;
    }
}
