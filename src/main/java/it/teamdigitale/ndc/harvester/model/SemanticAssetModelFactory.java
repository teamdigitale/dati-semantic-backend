package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SemanticAssetModelFactory {
    private final SemanticAssetsParser semanticAssetsParser;

    public ControlledVocabularyModel createControlledVocabulary(String ttlFile) {
        return new ControlledVocabularyModel(semanticAssetsParser, ttlFile);
    }
}
