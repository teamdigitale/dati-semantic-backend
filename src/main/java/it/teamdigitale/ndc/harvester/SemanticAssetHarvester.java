package it.teamdigitale.ndc.harvester;

import java.nio.file.Path;

public interface SemanticAssetHarvester {
    void harvest(String repoUrl, Path rootPath);
}
