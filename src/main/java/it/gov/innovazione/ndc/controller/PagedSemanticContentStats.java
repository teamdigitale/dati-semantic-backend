package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.PageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PagedSemanticContentStats {
    private static final Map<String, Function<SemanticContentStats, Object>> EXTRACTORS = Stream.<Pair<String, Function<SemanticContentStats, Object>>>of(
                    Pair.of("statId", SemanticContentStats::getId),
                    Pair.of("harvesterRunId", SemanticContentStats::getHarvesterRunId),
                    Pair.of("harvestedAt", getFromHarvesterRun(HarvesterRun::getStartedAt)),
                    Pair.of("harvestedRepoUrl", getFromHarvesterRun(HarvesterRun::getRepositoryUrl)),
                    Pair.of("harvestedRevision", getFromHarvesterRun(HarvesterRun::getRevision)),
                    Pair.of("resourceUri", SemanticContentStats::getResourceUri),
                    Pair.of("resourceType", SemanticContentStats::getResourceType),
                    Pair.of("rightHolder", SemanticContentStats::getRightHolder),
                    Pair.of("issuedOn", SemanticContentStats::getIssuedOn),
                    Pair.of("modifiedOn", SemanticContentStats::getModifiedOn),
                    Pair.of("hasErrors", SemanticContentStats::isHasErrors),
                    Pair.of("hasWarnings", SemanticContentStats::isHasWarnings),
                    Pair.of("status", SemanticContentStats::getStatusType))
            .collect(Collectors.toMap(
                    Pair::getLeft,
                    Pair::getRight,
                    (a, b) -> a,
                    LinkedHashMap::new));
    private final List<String> headers;
    private final List<List<String>> content;
    private final Integer page;
    private final Integer pageSize;
    private final Integer total;

    private static Function<SemanticContentStats, Object> getFromHarvesterRun(Function<HarvesterRun, Object> mapper) {
        return scs -> Optional.ofNullable(scs)
                .map(SemanticContentStats::getHarvesterRun)
                .map(mapper)
                .orElse(null);
    }

    public static PagedSemanticContentStats of(List<SemanticContentStats> content, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);

        return new PagedSemanticContentStats(
                List.copyOf(EXTRACTORS.keySet()),
                asStringList(
                        content.stream()
                                .skip(pageRequest.getOffset())
                                .limit(pageRequest.getPageSize())),
                page,
                pageSize,
                content.size());
    }

    private static List<List<String>> asStringList(Stream<SemanticContentStats> semanticContentStatsStream) {
        return semanticContentStatsStream
                .map(PagedSemanticContentStats::asStringList)
                .toList();
    }

    private static List<String> asStringList(SemanticContentStats scs) {
        return EXTRACTORS.values().stream()
                .map(extractor -> extractor.apply(scs))
                .map(String::valueOf)
                .toList();
    }
}
