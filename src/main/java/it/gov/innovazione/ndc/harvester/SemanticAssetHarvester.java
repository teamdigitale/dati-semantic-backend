package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.model.harvester.Repository;

import java.nio.file.Path;

public interface SemanticAssetHarvester {
    SemanticAssetType getType();

    void cleanUpBeforeHarvesting(String repoUrl);

    void harvest(Repository repository, Path rootPath);
}
