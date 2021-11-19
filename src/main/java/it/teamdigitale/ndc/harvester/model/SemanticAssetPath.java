package it.teamdigitale.ndc.harvester.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class SemanticAssetPath {
    private final String ttlPath;

    public SemanticAssetPath(String ttlPath) {
        this.ttlPath = ttlPath;
    }
}
