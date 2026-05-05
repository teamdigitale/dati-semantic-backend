package it.gov.innovazione.ndc.harvester.model;

import lombok.EqualsAndHashCode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode
public class CvPath extends SemanticAssetPath {
    private final String csvPath;
    private final String dbPath;

    public CvPath(String ttlPath, String csvPath) {
        this(ttlPath, csvPath, null);
    }

    public CvPath(String ttlPath, String csvPath, String dbPath) {
        super(ttlPath);
        this.csvPath = csvPath;
        this.dbPath = dbPath;
    }

    public Optional<String> getCsvPath() {
        return Optional.ofNullable(csvPath);
    }

    public Optional<String> getDbPath() {
        return Optional.ofNullable(dbPath);
    }

    public static CvPath of(String ttlPath, String csvPath) {
        return new CvPath(ttlPath, csvPath);
    }

    public static CvPath of(String ttlPath, String csvPath, String dbPath) {
        return new CvPath(ttlPath, csvPath, dbPath);
    }

    @Override
    public List<File> getAllFiles() {
        List<File> files = new ArrayList<>();
        files.add(new File(getTtlPath()));
        getCsvPath().ifPresent(p -> files.add(new File(p)));
        getDbPath().ifPresent(p -> files.add(new File(p)));
        return files;
    }

    @Override
    public String toString() {
        return "CvPath{ttlPath='" + ttlPath + "', csvPath='" + csvPath + "', dbPath='" + dbPath + "'}";
    }
}
