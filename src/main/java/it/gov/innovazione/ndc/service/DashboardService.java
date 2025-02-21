package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.service.SemanticContentStatsService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.SUCCESS;
import static java.util.Comparator.comparing;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final RepositoryService repositoryService;
    private final HarvesterRunService harvesterRunService;
    private final SemanticContentStatsService semanticContentStatsService;

    private List<HarvesterRun> allRuns;
    private List<SemanticContentStats> allStats;
    private Map<String, Repository> repoById;

    public synchronized List<SemanticContentStats> populateData() {
        allStats = semanticContentStatsService.getRawStats();
        allRuns = harvesterRunService.getAllRuns();
        repoById = repositoryService.getActiveRepos()
                .stream()
                .collect(toMap(Repository::getId, Function.identity()));

        Map<String, HarvesterRun> runsById = allRuns
                .stream()
                .filter(run -> SUCCESS.equals(run.getStatus()))
                .collect(toMap(HarvesterRun::getId, run -> run));

        Map<HarvesterRun, List<SemanticContentStats>> statsByRun = allStats.stream()
                .collect(groupingBy(
                        stats -> runsById.get(stats.getHarvesterRunId())));

        Map<Instant, List<SemanticContentStats>> statsByDate = allStats.stream()
                .collect(groupingBy(semanticContentStats -> {
                    HarvesterRun run = runsById.get(semanticContentStats.getHarvesterRunId());
                    return run.getStartedAt();
                }));

        List<RightsHolder> rightHoldersWithSingleName = repositoryService.getRightHoldersWithSingleName();
        log.info("Raw stats: {}", allStats);

        List<SemanticContentStats> a = getSnapshotAt(LocalDate.of(2026, 1, 1));
        List<Object[]> disaggregate1 = disaggregate(List.of(SemanticContentStats::getResourceType), a);
        List<Object[]> disaggregate2 = disaggregate(List.of(SemanticContentStats::getRightHolder), a);
        List<Object[]> disaggregate3 = disaggregate(List.of(
                SemanticContentStats::getResourceType,
                SemanticContentStats::getRightHolder,
                SemanticContentStats::getStatusType), a);

        List<SemanticContentStats> b = getSnapshotAt(LocalDate.of(2025, 1, 1));
        List<SemanticContentStats> c = getSnapshotAt(LocalDate.of(2024, 1, 1));
        List<SemanticContentStats> d = getSnapshotAt(LocalDate.of(2023, 1, 1));

        return allStats;
    }

    public List<Object[]> disaggregate(List<Function<SemanticContentStats, Object>> dimensions, List<SemanticContentStats> rawRecords) {
        return rawRecords.stream()
                .collect(groupingBy(
                        stats -> toDimensions(stats, dimensions),
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    List<Object> dimensionsValues = new ArrayList<>(entry.getKey());
                    dimensionsValues.add(entry.getValue());
                    return dimensionsValues.toArray();
                })
                .toList();
    }

    private List<String> toDimensions(SemanticContentStats stats, List<Function<SemanticContentStats, Object>> dimensions) {
        return dimensions.stream()
                .map(dimension -> dimension.apply(stats))
                .map(Object::toString)
                .toList();
    }

    public List<SemanticContentStats> getSnapshotAt(LocalDate localDate) {
        Set<String> harvesterRunIds = getLatestRunsByRepoAt(localDate).values()
                .stream()
                .map(HarvesterRun::getId)
                .collect(Collectors.toSet());
        return allStats.stream()
                .filter(stats -> harvesterRunIds.contains(stats.getHarvesterRunId()))
                .toList();
    }

    private Map<Repository, HarvesterRun> getLatestRunsByRepoAt(LocalDate localDate) {
        Instant localDateInstant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return allRuns.stream()
                .filter(run -> run.getStartedAt().isBefore(localDateInstant))
                .collect(toMap(run -> repoById.get(run.getRepositoryId()),
                        Function.identity(),
                        maxBy(comparing(HarvesterRun::getStartedAt))));
    }

    @PostConstruct
    public void init() {
        log.info("DashboardService initialized");
        populateData();
    }
}
