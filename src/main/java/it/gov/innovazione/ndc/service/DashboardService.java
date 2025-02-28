package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.AggregateDashboardResponse;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardRepo dashboardRepo;

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

    public Object getAggregateData(DateParams dateParams, List<DimensionalItem> dimensionalItems, List<Filter> filters) {
        List<LocalDate> localDates = new ArrayList<>();
        for (LocalDate date = dateParams.getStartDate();
             date.isBefore(dateParams.getEndDate().plusDays(1));
             date = dateParams.getDateIncrement().apply(date)) {
            localDates.add(date.with(dateParams.granularity.getAdjuster()));
        }

        Map<LocalDate, List<SemanticContentStats>> statsByDate = getSnapshotAt(localDates);

        Map<LocalDate, Map<List<String>, Long>> byDate = withFilledGaps(statsByDate.entrySet()
                        .stream()
                        .collect(toMap(Map.Entry::getKey, entry ->
                                disaggregate(
                                        dimensionalItems,
                                        filters,
                                        entry.getValue()))),
                localDates);

        return byDate
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> convertEntryToRow(entry, dateParams.getDateFormatter()))
                .flatMap(List::stream)
                .toList();
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

    public static List<Object[]> convertEntryToRow(Map.Entry<LocalDate, Map<List<String>, Long>> entry, Function<LocalDate, String> dateFormatter) {
        List<Object[]> result = new ArrayList<>();
        for (Map.Entry<List<String>, Long> entries : entry.getValue().entrySet()) {
            Object[] row = new Object[entries.getKey().size() + 2];
            row[0] = dateFormatter.apply(entry.getKey());
            for (int i = 0; i < entries.getKey().size(); i++) {
                row[i + 1] = entries.getKey().get(i);
            }
            row[row.length - 1] = entries.getValue();
            result.add(row);
        }
        return result;
    }

    @PostConstruct
    void startup() {
        getAggregateData(
                DateParams.of(LocalDate.of(2020, 1, 1), LocalDate.now(), Granularity.YEARS),
                List.of(DimensionalItem.RESOURCE_TYPE),
                List.of());
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


    @RequiredArgsConstructor(staticName = "of")
    public static class DateParams {

        private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2023, 6, 1);

        private final LocalDate startDate;
        private final LocalDate endDate;
        private final Granularity granularity;

        public LocalDate getStartDate() {
            return Optional.ofNullable(startDate)
                    .orElse(DEFAULT_START_DATE);
        }

        public LocalDate getEndDate() {
            return Optional.ofNullable(endDate)
                    .orElse(LocalDate.now())
                    .with(granularity.getAdjuster());
        }

        public Function<LocalDate, LocalDate> getDateIncrement() {
            return localDate -> localDate.plus(1, granularity.getUnit());
        }

        public Function<LocalDate, String> getDateFormatter() {
            return granularity.getFormatter();
        }
    }

    @RequiredArgsConstructor
    @Getter
    public enum Granularity {
        DAYS(ChronoUnit.DAYS, a -> a, date -> String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear())),
        YEARS(ChronoUnit.YEARS, TemporalAdjusters.lastDayOfYear(), date -> String.valueOf(date.getYear())),
        MONTHS(ChronoUnit.MONTHS, TemporalAdjusters.lastDayOfMonth(), date -> String.format("%02d/%d", date.getMonthValue(), date.getYear())),
        WEEKS_IN_MONTH(ChronoUnit.WEEKS,
                TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY),
                date ->
                        String.format("W%d/%02d/%d",
                                date.get(WeekFields.of(DayOfWeek.SUNDAY, 1).weekOfMonth()),
                                date.getMonthValue(),
                                date.getYear())),
        WEEKS_IN_YEAR(ChronoUnit.WEEKS,
                TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY),
                date ->
                        String.format("W%d/%d",
                                date.get(WeekFields.of(DayOfWeek.SUNDAY, 1).weekOfYear()),
                                date.getYear()));

        private final TemporalUnit unit;
        private final TemporalAdjuster adjuster;
        private final Function<LocalDate, String> formatter;
    }
}
