package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class SemanticAssetPathProcessor<P extends SemanticAssetPath> {
    protected final SemanticAssetModelFactory modelFactory;

    public abstract void process(P path);
}
