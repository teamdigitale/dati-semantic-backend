package it.teamdigitale.ndc.harvester.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CvPath extends SemanticAssetPath {
    private final String csvPath;

    public CvPath(String csvPath, String ttlPath) {
        super(ttlPath);
        this.csvPath = csvPath;
    }

    public static CvPath of(String csvPath, String ttlPath) {
        return new CvPath(csvPath, ttlPath);
    }
}
