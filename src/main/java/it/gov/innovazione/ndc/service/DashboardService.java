package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.AggregateDashboardResponse;
import it.gov.innovazione.ndc.controller.date.DateParameter;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingLong;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardRepo dashboardRepo;

    public static List<List<Object>> convertCountEntryToRow(Map.Entry<LocalDate, Map<List<String>, Long>> entry, Function<LocalDate, String> dateFormatter) {
        return convertEntryToRow(entry, dateFormatter, List::add);
    }

    public static List<List<Object>> convertTimeEntryToRow(Map.Entry<LocalDate, Map<List<String>, LongSummaryStatistics>> entry, Function<LocalDate, String> dateFormatter) {
        return convertEntryToRow(
                entry, dateFormatter,
                (row, stats) -> {
                    if (Objects.nonNull(stats)) {
                        row.add(stats.getMin());
                        row.add(stats.getMax());
                        row.add(stats.getAverage());
                        row.add(stats.getCount());
                        return;
                    }
                    row.add(0L);
                    row.add(0L);
                    row.add(0.0);
                    row.add(0L);
                });
    }

    private static <T> List<List<Object>> convertEntryToRow(
            Map.Entry<LocalDate, Map<List<String>, T>> entry,
            Function<LocalDate, String> dateFormatter, BiConsumer<List<Object>, T> valueConsumer) {
        List<List<Object>> result = new ArrayList<>();
        for (Map.Entry<List<String>, T> entries : entry.getValue().entrySet()) {
            List<Object> row = new ArrayList<>();
            row.add(dateFormatter.apply(entry.getKey()));
            row.addAll(entries.getKey());
            valueConsumer.accept(row, entries.getValue());
            result.add(row);
        }
        return result;
    }

    private static Instant fromLocalDate(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    public Map<List<String>, Long> disaggregate(
            List<DimensionalItem.CountDataDimensionalItem> countDataDimensionalItems,
            List<DimensionalItem.Filter<SemanticContentStats>> filters,
            List<SemanticContentStats> rawRecords) {
        return rawRecords.stream()
                .filter(stats -> filters.stream().allMatch(filter -> filter.test(stats)))
                .collect(groupingBy(
                        stats -> toDimensions(stats, countDataDimensionalItems),
                        Collectors.counting()));
    }

    public Map<List<String>, LongSummaryStatistics> disaggregateTimes(
            List<DimensionalItem.TimeDataDimensionalItem> countDataDimensionalItems,
            List<DimensionalItem.Filter<HarvesterRun>> filters,
            List<HarvesterRun> rawRecords) {
        return rawRecords.stream()
                .filter(stats -> filters.stream().allMatch(filter -> filter.test(stats)))
                .collect(groupingBy(
                        stats -> toDimensions(stats, countDataDimensionalItems),
                        Collectors.collectingAndThen(
                                summarizingLong(value -> Duration.between(value.getStartedAt(), value.getEndedAt()).getSeconds()),
                                stats -> stats)));
    }

    private List<String> toDimensions(SemanticContentStats stats, List<DimensionalItem.CountDataDimensionalItem> countDataDimensionalItems) {
        return countDataDimensionalItems.stream()
                .map(countDataDimensionalItem -> countDataDimensionalItem.extract(stats))
                .map(Object::toString)
                .toList();
    }

    private List<String> toDimensions(HarvesterRun stats, List<DimensionalItem.TimeDataDimensionalItem> timeDataDimensionalItems) {
        return timeDataDimensionalItems.stream()
                .map(countDataDimensionalItem -> countDataDimensionalItem.extract(stats))
                .map(Object::toString)
                .toList();
    }

    public Map<LocalDate, List<SemanticContentStats>> getSnapshotAt(DateParameter dateParameter) {
        Map<LocalDate, List<SemanticContentStats>> statsByDate = new LinkedHashMap<>();
        for (LocalDate date : dateParameter.getDates()) {
            LocalDate nextDate = dateParameter.getDateIncrement().apply(date);
            Set<String> harvesterRunIds = getLatestRunsByRepoAt(nextDate).values()
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

    public AggregateDashboardResponse getAggregateTimeData(
            DateParameter dateParams,
            List<DimensionalItem.TimeDataDimensionalItem> timeDataDimensionalItems,
            List<DimensionalItem.Filter<HarvesterRun>> filters) {
        List<LocalDate> dates = dateParams.getDates();

        Map<LocalDate, List<HarvesterRun>> statsByDate = getRunSnapshotAt(dates);

        Map<LocalDate, Map<List<String>, LongSummaryStatistics>> byDate = withFilledGaps(statsByDate.entrySet()
                        .stream()
                        .collect(toMap(
                                Map.Entry::getKey,
                                entry ->
                                        disaggregateTimes(
                                                timeDataDimensionalItems,
                                                filters,
                                                entry.getValue()))),
                dates,
                null);

        List<List<Object>> data = byDate
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> convertTimeEntryToRow(entry, dateParams.getDateFormatter()))
                .flatMap(List::stream)
                .toList();

        return AggregateDashboardResponse.builder()
                .headers(Stream.of(
                                List.of("DATE"),
                                timeDataDimensionalItems.stream()
                                        .map(DimensionalItem.TimeDataDimensionalItem::name)
                                        .toList(),
                                List.of("MIN", "MAX", "AVERAGE", "COUNT"))
                        .flatMap(List::stream)
                        .toList())
                .rows(data)
                .build();
    }

    private Map<LocalDate, List<HarvesterRun>> getRunSnapshotAt(List<LocalDate> dates) {
        Map<LocalDate, List<HarvesterRun>> statsByDate = new LinkedHashMap<>();
        for (LocalDate date : dates) {
            statsByDate.put(date, new ArrayList<>());
        }
        LocalDate maxDate = dates.get(dates.size() - 1);
        for (HarvesterRun run : dashboardRepo.getAllRuns()) {
            Instant startedAt = run.getStartedAt();
            boolean added = false;
            for (int i = 0; i < dates.size(); i++) {
                LocalDate upperDate = dates.get(i);
                LocalDate lowerDate = i > 0 ? dates.get(i - 1) : LocalDate.MIN;

                Instant upper = fromLocalDate(upperDate);
                Instant lower = fromLocalDate(lowerDate);

                if (!startedAt.isBefore(lower) && startedAt.isBefore(upper)) {
                    statsByDate.get(lowerDate).add(run);
                    added = true;
                    break;
                }
            }
            if (!added && startedAt.isAfter(fromLocalDate(maxDate))) {
                statsByDate.get(maxDate).add(run);
            }
        }
        return statsByDate;
    }

    public AggregateDashboardResponse getAggregateCountData(
            DateParameter dateParams,
            List<DimensionalItem.CountDataDimensionalItem> countDataDimensionalItems,
            List<DimensionalItem.Filter<SemanticContentStats>> filters) {

        Map<LocalDate, List<SemanticContentStats>> statsByDate = getSnapshotAt(dateParams);

        List<LocalDate> dates = dateParams.getDates();

        Map<LocalDate, Map<List<String>, Long>> byDate = withFilledGaps(statsByDate.entrySet()
                        .stream()
                        .collect(toMap(Map.Entry::getKey, entry ->
                                disaggregate(
                                        countDataDimensionalItems,
                                        filters,
                                        entry.getValue()))),
                dates,
                0L);

        List<List<Object>> data = byDate
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> convertCountEntryToRow(entry, dateParams.getDateFormatter()))
                .flatMap(List::stream)
                .toList();

        return AggregateDashboardResponse.builder()
                .headers(Stream.of(
                                List.of("DATE"),
                                countDataDimensionalItems.stream()
                                        .map(DimensionalItem.CountDataDimensionalItem::name)
                                        .toList(),
                                List.of("VAUE"))
                        .flatMap(List::stream)
                        .toList())
                .rows(data)
                .build();
    }


    private <T> Map<LocalDate, Map<List<String>, T>> withFilledGaps(Map<LocalDate, Map<List<String>, T>> byDate, List<LocalDate> localDates, T defaultValue) {
        List<List<String>> allDimensionsCombinations = byDate.values().stream()
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .toList();

        Map<LocalDate, Map<List<String>, T>> withFilledGaps = new HashMap<>();

        for (LocalDate date : localDates) {
            Map<List<String>, T> entry = new HashMap<>();
            withFilledGaps.put(date, entry);
            for (List<String> dimensionsCombination : allDimensionsCombinations) {
                T value = defaultValue;
                if (byDate.containsKey(date) && byDate.get(date).containsKey(dimensionsCombination)) {
                    value = byDate.get(date).get(dimensionsCombination);
                }
                entry.put(new ArrayList<>(dimensionsCombination), value);
            }
        }
        return withFilledGaps;
    }

    public List<SemanticContentStats> getRawData(LocalDate startDate, LocalDate endDate, List<DimensionalItem.Filter<SemanticContentStats>> filters) {
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
}
