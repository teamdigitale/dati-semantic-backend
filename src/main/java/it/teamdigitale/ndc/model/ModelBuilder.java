package it.teamdigitale.ndc.model;

import it.teamdigitale.ndc.gen.model.SemanticAssetSearchResult;
import it.teamdigitale.ndc.gen.model.SemanticAssetsSearchDto;
import lombok.Builder;

import java.util.List;

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
}
