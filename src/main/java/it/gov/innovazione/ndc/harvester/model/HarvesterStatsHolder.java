package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HarvesterStatsHolder {
    private final SemanticAssetMetadata metadata;
    private final SemanticAssetModelValidationContext.ValidationContextStats validationContextStats;
}
