package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;

public interface SemanticAssetPathProcessor<P extends SemanticAssetPath> {
    void process(String repoUrl, P path);
}
