package it.gov.innovazione.ndc.model;

import it.gov.innovazione.ndc.gen.dto.Direction;
import it.gov.innovazione.ndc.gen.dto.Problem;
import it.gov.innovazione.ndc.gen.dto.SearchResult;
import it.gov.innovazione.ndc.gen.dto.SearchResultItem;
import it.gov.innovazione.ndc.gen.dto.SortBy;
import it.gov.innovazione.ndc.gen.dto.VocabulariesResult;
import it.gov.innovazione.ndc.gen.dto.VocabularyData;
import it.gov.innovazione.ndc.gen.dto.VocabularySummary;
import it.gov.innovazione.ndc.gen.dto.VocabularySummaryLinksInner;
import lombok.Builder;
import lombok.SneakyThrows;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;

public class Builders {
    private Builders() {
    }

    @Builder(builderMethodName = "searchResult")
    public static SearchResult newSearchResult(Integer totalCount, Integer limit, Integer offset, Pageable pageable, List<SearchResultItem> data) {
        SearchResult result = new SearchResult();

        SortBy sortBy =
                Optional.ofNullable(pageable)
                        .map(Pageable::getSort)
                        .flatMap(orders -> orders.stream()
                                .findFirst()
                                .map(Sort.Order::getProperty)
                                .map(property -> LOWER_CAMEL.to(UPPER_UNDERSCORE, property))
                                .map(SortBy::valueOf))
                        .orElse(null);

        Direction direction =
                Optional.ofNullable(pageable)
                        .map(Pageable::getSort)
                        .flatMap(orders -> orders.stream()
                                .findFirst()
                                .map(Sort.Order::getDirection)
                                .map(dir -> dir == Sort.Direction.ASC ? Direction.ASC : Direction.DESC))
                        .orElse(null);

        result.setTotalCount(totalCount);
        result.setLimit(limit);
        result.setOffset(offset);
        result.setData(data);
        result.setSortBy(sortBy);
        result.setDirection(direction);

        return result;
    }

    @Builder(builderMethodName = "vocabularyData")
    public static VocabularyData newVocabularyData(Integer totalResults, Integer limit, Integer offset, List<Map<String, String>> data) {
        VocabularyData dto = new VocabularyData();

        dto.setTotalResults(totalResults);
        dto.setLimit(limit);
        dto.setOffset(offset);
        dto.setData(data);

        return dto;
    }

    @Builder(builderMethodName = "vocabulariesResult")
    public static VocabulariesResult newVocabulariesResult(Integer totalCount, Integer limit, Integer offset, List<VocabularySummary> data) {
        VocabulariesResult result = new VocabulariesResult();

        result.setTotalCount(totalCount);
        result.setLimit(limit);
        result.setOffset(offset);
        result.setData(data);

        return result;
    }

    @Builder(builderMethodName = "vocabularySummary")
    public static VocabularySummary newVocabularySummary(String title, String description, String agencyId, String keyConcept, String endpointUrl) {
        VocabularySummary dto = new VocabularySummary();

        dto.setTitle(title);
        dto.setDescription(description);
        dto.setAgencyId(agencyId);
        dto.setKeyConcept(keyConcept);
        dto.setLinks(getVocabularySummaryLinks(endpointUrl));

        return dto;
    }

    public static List<VocabularySummaryLinksInner> getVocabularySummaryLinks(String endpointUrl) {
        VocabularySummaryLinksInner link = new VocabularySummaryLinksInner();
        link.setType("GET");
        link.setHref(endpointUrl);
        link.setRel("items");
        return List.of(link);
    }

    @Builder(builderMethodName = "problem")
    @SneakyThrows
    public static Problem build(String errorClass, String title, HttpStatus status) {
        Problem problem = new Problem();
        problem.setStatus(status.value());
        problem.setType(new URI("https://www.schema.gov.it/tech/errors/" + errorClass));
        problem.setTitle(title);
        problem.setTimestamp(LocalDateTime.now().toString());
        return problem;
    }
}
