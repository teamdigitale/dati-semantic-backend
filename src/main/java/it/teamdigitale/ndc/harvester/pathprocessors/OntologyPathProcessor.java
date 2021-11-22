package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OntologyPathProcessor extends SemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {

    private final SemanticAssetModelFactory modelFactory;

    @Override
    protected OntologyModel loadModel(String ttlFile) {
        return modelFactory.createOntology(ttlFile);
    }
}
