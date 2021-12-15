package it.teamdigitale.ndc.model;

import it.teamdigitale.ndc.gen.model.SemanticAssetSearchResult;
import it.teamdigitale.ndc.gen.model.SemanticAssetsSearchDto;
import it.teamdigitale.ndc.gen.model.VocabularyDataDto;
import lombok.Builder;

import java.util.List;
import java.util.Map;

public class ModelBuilder {
    private ModelBuilder() {
    }

    @Builder(builderMethodName = "searchResultBuilder")
    public static SemanticAssetSearchResult newSearchResult(Long totalCount, Integer limit, Long offset, List<SemanticAssetsSearchDto> data) {
        SemanticAssetSearchResult result = new SemanticAssetSearchResult();

        result.setTotalCount(totalCount);
        result.setLimit(limit);
        result.setOffset(offset);
        result.setData(data);

        return result;
    }
    
    @Builder(builderMethodName = "vocabularyDataBuilder")
    public static VocabularyDataDto newVocabularyDataDto(Long totalResults, Integer limit, Long offset, List<Map<String, String>> data) {
        VocabularyDataDto dto = new VocabularyDataDto();

        dto.setTotalResults(totalResults);
        dto.setLimit(limit);
        dto.setOffset(offset);
        dto.setData(data);

        return dto;
    }
}
