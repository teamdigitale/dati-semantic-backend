package it.gov.innovazione.ndc.controller.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.model.audit.ChangeKind;
import it.gov.innovazione.ndc.model.audit.ResourceDelta;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@Data
@Builder
public class ResourceDeltaItem {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String assetIri;
    private final SemanticAssetType assetType;
    private final ChangeKind changeKind;
    private final JsonNode summary;
    private final Instant createdAt;
    private final String harvesterRunId;

    public static ResourceDeltaItem of(ResourceDelta d) {
        return ResourceDeltaItem.builder()
                .assetIri(d.getAssetIri())
                .assetType(d.getAssetType())
                .changeKind(d.getChangeKind())
                .summary(parseJson(d.getSummaryJson()))
                .createdAt(d.getCreatedAt())
                .harvesterRunId(d.getHarvesterRunId())
                .build();
    }

    private static JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return JSON.readTree(raw);
        } catch (JsonProcessingException e) {
            log.warn("Could not parse summary JSON, returning as raw text: {}", e.getMessage());
            return JSON.getNodeFactory().textNode(raw);
        }
    }
}
