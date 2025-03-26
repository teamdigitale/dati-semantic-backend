package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.AggregateDashboardResponse;
import it.gov.innovazione.ndc.controller.date.DateParameter;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardRepo dashboardRepo;

    public static List<List<Object>> convertEntryToRow(Map.Entry<LocalDate, Map<List<String>, Long>> entry, Function<LocalDate, String> dateFormatter) {
        List<List<Object>> result = new ArrayList<>();
        for (Map.Entry<List<String>, Long> entries : entry.getValue().entrySet()) {
            List<Object> row = new ArrayList<>();
            row.add(dateFormatter.apply(entry.getKey()));
            for (int i = 0; i < entries.getKey().size(); i++) {
                row.add(entries.getKey().get(i));
            }
            row.add(entries.getValue());
            result.add(row);
        }
        return result;
    }

    public Map<List<String>, Long> disaggregate(
            List<DimensionalItem> dimensionalItems,
            List<Filter> filters,
            List<SemanticContentStats> rawRecords) {
        return rawRecords.stream()
                .filter(stats -> filters.stream().allMatch(filter -> filter.test(stats)))
                .collect(groupingBy(
                        stats -> toDimensions(stats, dimensionalItems),
                        Collectors.counting()));
    }

    private List<String> toDimensions(SemanticContentStats stats, List<DimensionalItem> dimensionalItems) {
        return dimensionalItems.stream()
                .map(dimensionalItem -> dimensionalItem.extract(stats))
                .map(Object::toString)
                .toList();
    }

    public List<SemanticContentStats> getSnapshotAt(LocalDate localDate) {
        return getSnapshotAt(List.of(localDate)).get(localDate);
    }

    public Map<LocalDate, List<SemanticContentStats>> getSnapshotAt(List<LocalDate> localDate) {
        Map<LocalDate, List<SemanticContentStats>> statsByDate = new LinkedHashMap<>();
        for (LocalDate date : localDate) {
            Set<String> harvesterRunIds = getLatestRunsByRepoAt(date).values()
                    .stream()
                    .map(HarvesterRun::getId)
                    .collect(Collectors.toSet());
            statsByDate.put(date, dashboardRepo.getAllStats().stream()
                    .filter(stats -> harvesterRunIds.contains(stats.getHarvesterRunId()))
                    .toList());
        }
        return statsByDate;
    }

    private Map<Repository, HarvesterRun> getLatestRunsByRepoAt(LocalDate localDate) {
        Instant localDateInstant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return dashboardRepo.getAllRuns().stream()
                .filter(run -> run.getStartedAt().isBefore(localDateInstant))
                .collect(toMap(run -> dashboardRepo.getRepoById().get(run.getRepositoryId()),
                        Function.identity(),
                        maxBy(comparing(HarvesterRun::getStartedAt))));
    }

    public AggregateDashboardResponse getAggregateData(DateParameter dateParams, List<DimensionalItem> dimensionalItems, List<Filter> filters) {
        List<LocalDate> dates = dateParams.getDates();

        Map<LocalDate, List<SemanticContentStats>> statsByDate = getSnapshotAt(dates);

        Map<LocalDate, Map<List<String>, Long>> byDate = withFilledGaps(statsByDate.entrySet()
                        .stream()
                        .collect(toMap(Map.Entry::getKey, entry ->
                                disaggregate(
                                        dimensionalItems,
                                        filters,
                                        entry.getValue()))),
                dates);

        List<List<Object>> data = byDate
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> convertEntryToRow(entry, dateParams.getDateFormatter()))
                .flatMap(List::stream)
                .toList();

        return AggregateDashboardResponse.builder()
                .headers(
                        Stream.of(
                                        List.of("DATE"),
                                        dimensionalItems.stream()
                                                .map(DimensionalItem::name)
                                                .toList(),
                                        List.of("VAUE"))
                                .flatMap(List::stream)
                                .toList())
                .rows(data)
                .build();
    }

    private Map<LocalDate, Map<List<String>, Long>> withFilledGaps(Map<LocalDate, Map<List<String>, Long>> byDate, List<LocalDate> localDates) {
        List<List<String>> allDimensionsCombinations = byDate.values().stream()
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .toList();

        Map<LocalDate, Map<List<String>, Long>> withFilledGaps = new HashMap<>();

        for (LocalDate date : localDates) {
            Map<List<String>, Long> entry = new HashMap<>();
            withFilledGaps.put(date, entry);
            for (List<String> dimensionsCombination : allDimensionsCombinations) {
                Long value = 0L;
                if (byDate.containsKey(date) && byDate.get(date).containsKey(dimensionsCombination)) {
                    value = byDate.get(date).get(dimensionsCombination);
                }
                entry.put(new ArrayList<>(dimensionsCombination), value);
            }
        }
        return withFilledGaps;
    }

    public List<SemanticContentStats> getRawData(LocalDate startDate, LocalDate endDate, List<Filter> filters) {
        List<String> runIdWithinDates = dashboardRepo.getAllRuns().stream()
                .filter(run -> run.getStartedAt().isAfter(
                        Optional.ofNullable(startDate)
                                .orElse(LocalDate.of(1970, 1, 1))
                                .atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .filter(run -> run.getStartedAt().isBefore(
                        Optional.ofNullable(endDate)
                                .orElse(LocalDate.now())
                                .atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .map(HarvesterRun::getId)
                .toList();
        return dashboardRepo.getAllStats().stream()
                .filter(stats -> runIdWithinDates.contains(stats.getHarvesterRunId()))
                .filter(stats -> filters.stream().allMatch(filter -> filter.test(stats)))
                .map(scs -> Pair.of(
                        dashboardRepo.getRunById().get(scs.getHarvesterRunId()).getStartedAt(),
                        scs))
                .sorted(comparing(Pair::getLeft))
                .map(Pair::getRight)
                .map(this::enrichWithRun)
                .toList();
    }

    private SemanticContentStats enrichWithRun(SemanticContentStats semanticContentStats) {
        return semanticContentStats.withHarvesterRun(dashboardRepo.getRunById().get(semanticContentStats.getHarvesterRunId()));
    }

    @RequiredArgsConstructor
    public enum DimensionalItem {
        RESOURCE_TYPE(SemanticContentStats::getResourceType, true),
        RIGHT_HOLDER(SemanticContentStats::getRightHolder, true),
        STATUS(SemanticContentStats::getStatusType, true),
        HAS_ERRORS(SemanticContentStats::isHasErrors, false),
        HAS_WARNINGS(SemanticContentStats::isHasWarnings, false);

        private final Function<SemanticContentStats, Object> dimensionExtractor;
        private final boolean dimensionable;

        public Object extract(SemanticContentStats stats) {
            return dimensionExtractor.apply(stats);
        }

        public boolean test(SemanticContentStats stats, String value, boolean exactMatch) {
            if (exactMatch) {
                return dimensionExtractor.apply(stats).toString().equals(value);
            }
            return dimensionExtractor.apply(stats).toString().equalsIgnoreCase(value);
        }
    }

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    public static class Filter {
        private final DimensionalItem dimensionalItem;
        private final List<String> values;

        public boolean test(SemanticContentStats stats) {
            return values.stream().anyMatch(value -> dimensionalItem.test(stats, value, false));
        }

    }
}
