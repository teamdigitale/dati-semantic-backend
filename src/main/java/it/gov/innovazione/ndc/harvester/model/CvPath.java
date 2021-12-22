package it.gov.innovazione.ndc.harvester.model;

import lombok.EqualsAndHashCode;

import java.util.Optional;

@EqualsAndHashCode
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

    @Override
    public String toString() {
        return "CvPath{ttlPath='" + ttlPath + "', csvPath='" + csvPath + "'}";
    }
}
