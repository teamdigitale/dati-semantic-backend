package it.teamdigitale.ndc.model;

import it.teamdigitale.ndc.gen.dto.Problem;
import it.teamdigitale.ndc.gen.dto.SearchResult;
import it.teamdigitale.ndc.gen.dto.SearchResultItem;
import it.teamdigitale.ndc.gen.dto.VocabularyData;
import lombok.Builder;
import lombok.SneakyThrows;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Builders {
    private Builders() {
    }

    @Builder(builderMethodName = "searchResult")
    public static SearchResult newSearchResult(Long totalCount, Integer limit, Long offset, List<SearchResultItem> data) {
        SearchResult result = new SearchResult();

        result.setTotalCount(totalCount);
        result.setLimit(limit);
        result.setOffset(offset);
        result.setData(data);

        return result;
    }
    
    @Builder(builderMethodName = "vocabularyData")
    public static VocabularyData newVocabularyDataDto(Long totalResults, Integer limit, Long offset, List<Map<String, String>> data) {
        VocabularyData dto = new VocabularyData();

        dto.setTotalResults(totalResults);
        dto.setLimit(limit);
        dto.setOffset(offset);
        dto.setData(data);

        return dto;
    }

    @Builder(builderMethodName = "problem")
    @SneakyThrows
    public static Problem build(String errorClass, String title, int status) {
        Problem problem = new Problem();
        problem.setStatus(status);
        problem.setType(new URI("https://schema.gov.it/tech/errors/" + errorClass));
        problem.setTitle(title);
        problem.setTimestamp(LocalDateTime.now().toString());
        return problem;
    }
}
