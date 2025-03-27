package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static it.gov.innovazione.ndc.service.DimensionalItem.CountDataDimensionalItem.HAS_ERRORS;
import static it.gov.innovazione.ndc.service.DimensionalItem.CountDataDimensionalItem.HAS_WARNINGS;
import static it.gov.innovazione.ndc.service.DimensionalItem.CountDataDimensionalItem.RESOURCE_TYPE;
import static it.gov.innovazione.ndc.service.DimensionalItem.CountDataDimensionalItem.RIGHT_HOLDER;
import static it.gov.innovazione.ndc.service.DimensionalItem.CountDataDimensionalItem.STATUS;
import static it.gov.innovazione.ndc.service.DimensionalItem.TimeDataDimensionalItem.REPOSITORY_URL;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public interface DimensionalItem<T> {

    Function<T, Object> getDimensionExtractor();

    boolean isDimensionable();

    default Object extract(T t) {
        return getDimensionExtractor().apply(t);
    }

    default boolean test(T t, String value, boolean exactMatch) {
        if (exactMatch) {
            return getDimensionExtractor().apply(t).toString().equals(value);
        }
        return getDimensionExtractor().apply(t).toString().equalsIgnoreCase(value);
    }

    @Getter
    @RequiredArgsConstructor
    enum TimeDataDimensionalItem implements DimensionalItem<HarvesterRun> {
        REPOSITORY_URL(HarvesterRun::getRepositoryUrl, true);

        private final Function<HarvesterRun, Object> dimensionExtractor;
        private final boolean dimensionable;
    }

    @Getter
    @RequiredArgsConstructor
    enum CountDataDimensionalItem implements DimensionalItem<SemanticContentStats> {
        RESOURCE_TYPE(SemanticContentStats::getResourceType, true),
        RIGHT_HOLDER(SemanticContentStats::getRightHolder, true),
        STATUS(SemanticContentStats::getStatusType, true),
        HAS_ERRORS(SemanticContentStats::isHasErrors, false),
        HAS_WARNINGS(SemanticContentStats::isHasWarnings, false);

        private final Function<SemanticContentStats, Object> dimensionExtractor;
        private final boolean dimensionable;
    }

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    class Filter<T> {
        private final DimensionalItem<T> dimensionalItem;
        private final List<String> values;

        public boolean test(T t) {
            return values.stream()
                    .anyMatch(value -> dimensionalItem.test(t, value, false));
        }

        public static List<Filter<SemanticContentStats>> getSemanticContentFilters(
                List<String> status,
                List<String> resourceType,
                List<String> rightHolder,
                List<String> hasErrors,
                List<String> hasWarnings) {
            return makeFilters(Map.of(
                    STATUS, emptyIfNull(status),
                    RESOURCE_TYPE, emptyIfNull(resourceType),
                    RIGHT_HOLDER, emptyIfNull(rightHolder),
                    HAS_ERRORS, emptyIfNull(hasErrors),
                    HAS_WARNINGS, emptyIfNull(hasWarnings)));
        }

        public static List<Filter<HarvesterRun>> getTimeDataFilters(
                List<String> repositoryUrl) {
            return makeFilters(Map.of(
                    REPOSITORY_URL,
                    emptyIfNull(repositoryUrl)));
        }

        private static <T> List<Filter<T>> makeFilters(Map<DimensionalItem<T>, List<String>> filters) {
            return filters.entrySet().stream()
                    .filter(e -> isNotEmpty(e.getValue()))
                    .map(e -> Filter.of(e.getKey(), e.getValue()))
                    .toList();
        }
    }
}
