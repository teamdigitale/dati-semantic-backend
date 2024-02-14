package it.gov.innovazione.ndc.harvester.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.File;
import java.util.List;

@Getter
@EqualsAndHashCode
public class SemanticAssetPath {
    protected final String ttlPath;

    public SemanticAssetPath(String ttlPath) {
        this.ttlPath = ttlPath;
    }

    public static SemanticAssetPath of(String ttlPath) {
        return new SemanticAssetPath(ttlPath);
    }

    @Override
    public String toString() {
        return "SemanticAssetPath{ttlPath='" + ttlPath + "'}";
    }

    public List<File> getAllFiles() {
        return List.of(new File(ttlPath));
    }
}
