package it.teamdigitale.ndc.controller.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticAssetSearchResult {
    Integer totalPages;
    Integer pageNumber;
    List<SemanticAssetsSearchResultEntry> data;
}
