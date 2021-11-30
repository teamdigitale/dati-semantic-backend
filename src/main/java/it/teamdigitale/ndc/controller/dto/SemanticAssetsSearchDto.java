package it.teamdigitale.ndc.controller.dto;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticAssetsSearchDto {
    private String assetIri;
    private String title;
    private String description;
    private SemanticAssetType type;
    private LocalDate modified;
    private List<String> themes;
    private NodeSummary rightsHolder;

    public static SemanticAssetsSearchDto from(SemanticAssetMetadata metadata) {
        return SemanticAssetsSearchDto.builder()
            .assetIri(metadata.getIri())
            .title(metadata.getTitle())
            .description(metadata.getDescription())
            .type(metadata.getType())
            .modified(metadata.getModifiedOn())
            .themes(metadata.getThemes())
            .rightsHolder(metadata.getRightsHolder())
            .build();
    }
}
