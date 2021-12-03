package it.teamdigitale.ndc.controller.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticAssetSearchResult {
    Long totalCount;
    Integer limit;
    Long offset;
    List<SemanticAssetsSearchDto> data;
}
