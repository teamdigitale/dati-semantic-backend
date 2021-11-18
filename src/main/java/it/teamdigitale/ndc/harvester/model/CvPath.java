package it.teamdigitale.ndc.harvester.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class CvPath {
    private final String csvPath;
    private final String ttlPath;

    public CvPath(String csvPath, String ttlPath) {
        this.csvPath = csvPath;
        this.ttlPath = ttlPath;
    }

    public static CvPath of(String csvPath, String ttlPath) {
        return new CvPath(csvPath, ttlPath);
    }
}
