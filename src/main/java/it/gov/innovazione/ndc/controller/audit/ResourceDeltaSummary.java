package it.gov.innovazione.ndc.controller.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.model.audit.ChangeKind;
import lombok.Builder;
import lombok.Data;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class ResourceDeltaSummary {
    private final RunInfo run;
    private final Map<ChangeKind, Integer> byChangeKind;
    private final Map<SemanticAssetType, Integer> byAssetType;
    private final Map<SemanticAssetType, Map<ChangeKind, Integer>> crossTab;

    public static ResourceDeltaSummary empty(RunInfo run) {
        Map<ChangeKind, Integer> byKind = new EnumMap<>(ChangeKind.class);
        for (ChangeKind k : ChangeKind.values()) {
            byKind.put(k, 0);
        }
        Map<SemanticAssetType, Integer> byType = new EnumMap<>(SemanticAssetType.class);
        for (SemanticAssetType t : SemanticAssetType.values()) {
            byType.put(t, 0);
        }
        Map<SemanticAssetType, Map<ChangeKind, Integer>> cross = new LinkedHashMap<>();
        for (SemanticAssetType t : SemanticAssetType.values()) {
            Map<ChangeKind, Integer> row = new EnumMap<>(ChangeKind.class);
            for (ChangeKind k : ChangeKind.values()) {
                row.put(k, 0);
            }
            cross.put(t, row);
        }
        return ResourceDeltaSummary.builder()
                .run(run)
                .byChangeKind(byKind)
                .byAssetType(byType)
                .crossTab(cross)
                .build();
    }
}
