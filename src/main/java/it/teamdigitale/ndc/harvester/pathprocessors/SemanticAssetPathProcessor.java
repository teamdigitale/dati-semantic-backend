package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;

public interface SemanticAssetPathProcessor<P extends SemanticAssetPath> {
    void process(String repoUrl, P path);
}
