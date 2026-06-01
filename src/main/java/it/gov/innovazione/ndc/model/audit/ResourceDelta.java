package it.gov.innovazione.ndc.model.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ResourceDelta {
    private final String id;
    private final String harvesterRunId;
    private final String assetIri;
    private final SemanticAssetType assetType;
    private final ChangeKind changeKind;
    private final String summaryJson;
    private final Instant createdAt;
}
