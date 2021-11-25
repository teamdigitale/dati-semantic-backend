package it.teamdigitale.ndc.controller.dto;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticAssetsSearchDto {
    private String iri;
    private String title;
    private String description;
    private SemanticAssetType type;
    private LocalDate modified;
    private List<String> theme;
    private String rightsHolder;

    public static SemanticAssetsSearchDto from(SemanticAssetMetadata metadata) {
        return SemanticAssetsSearchDto.builder()
            .iri(metadata.getIri())
            .title(metadata.getTitle())
            .description(metadata.getDescription())
            .type(metadata.getType())
            .modified(metadata.getModified())
            .theme(metadata.getTheme())
            .rightsHolder(metadata.getRightsHolder())
            .build();
    }
}
