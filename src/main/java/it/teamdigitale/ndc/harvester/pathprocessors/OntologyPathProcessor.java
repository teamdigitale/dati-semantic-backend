package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OntologyPathProcessor extends SemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {
    private final SemanticAssetModelFactory modelFactory;

    public OntologyPathProcessor(TripleStoreRepository repository, SemanticAssetModelFactory modelFactory) {
        super(repository);
        this.modelFactory = modelFactory;
    }

    @Override
    protected OntologyModel loadModel(String ttlFile) {
        return modelFactory.createOntology(ttlFile);
    }
}
