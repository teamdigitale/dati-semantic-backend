package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.model.harvester.Repository;

import java.nio.file.Path;

public interface SemanticAssetHarvester {
    SemanticAssetType getType();

    void cleanUpBeforeHarvesting(String repoUrl, Instance instance);

    void harvest(Repository repository, Path rootPath);
}
