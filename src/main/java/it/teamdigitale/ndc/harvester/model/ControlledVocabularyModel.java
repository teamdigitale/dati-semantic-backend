package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import org.apache.jena.rdf.model.Resource;

public class ControlledVocabularyModel extends SemanticAssetModel {
    private final SemanticAssetsParser semanticAssetsParser;
    private final String ttlFile;

    public ControlledVocabularyModel(SemanticAssetsParser semanticAssetsParser, String ttlFile) {
        this.semanticAssetsParser = semanticAssetsParser;
        this.ttlFile = ttlFile;
    }

    public String getKeyConcept() {
        return semanticAssetsParser.getKeyConcept(getMainResource());
    }

    public String getRightsHolderId() {
        return semanticAssetsParser.getRightsHolderId(getMainResource());
    }

    public Resource getMainResource() {
        return semanticAssetsParser.getControlledVocabulary(ttlFile);
    }
}
