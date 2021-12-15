package it.teamdigitale.ndc.model;

import it.teamdigitale.ndc.gen.dto.SearchResult;
import it.teamdigitale.ndc.gen.dto.SearchResultItem;
import it.teamdigitale.ndc.gen.dto.VocabularyData;
import lombok.Builder;

import java.util.List;
import java.util.Map;

public class ModelBuilder {
    private ModelBuilder() {
    }

    @Builder(builderMethodName = "searchResultBuilder")
    public static SearchResult newSearchResult(Long totalCount, Integer limit, Long offset, List<SearchResultItem> data) {
        SearchResult result = new SearchResult();

        result.setTotalCount(totalCount);
        result.setLimit(limit);
        result.setOffset(offset);
        result.setData(data);

        return result;
    }
    
    @Builder(builderMethodName = "vocabularyDataBuilder")
    public static VocabularyData newVocabularyDataDto(Long totalResults, Integer limit, Long offset, List<Map<String, String>> data) {
        VocabularyData dto = new VocabularyData();

        dto.setTotalResults(totalResults);
        dto.setLimit(limit);
        dto.setOffset(offset);
        dto.setData(data);

        return dto;
    }
}
