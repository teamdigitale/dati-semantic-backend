package it.teamdigitale.ndc.harvester.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Optional;

@EqualsAndHashCode
@ToString
public class CvPath extends SemanticAssetPath {
    private final String csvPath;

    public CvPath(String ttlPath, String csvPath) {
        super(ttlPath);
        this.csvPath = csvPath;
    }

    public Optional<String> getCsvPath() {
        return Optional.ofNullable(csvPath);
    }

    public static CvPath of(String ttlPath, String csvPath) {
        return new CvPath(ttlPath, csvPath);
    }
}
