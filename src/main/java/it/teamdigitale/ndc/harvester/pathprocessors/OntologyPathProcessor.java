package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OntologyPathProcessor extends BaseSemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {
    private final SemanticAssetModelFactory modelFactory;

    public OntologyPathProcessor(TripleStoreRepository tripleStoreRepository, SemanticAssetModelFactory modelFactory,
                                 SemanticAssetMetadataRepository metadataRepository) {
        super(tripleStoreRepository, metadataRepository);
        this.modelFactory = modelFactory;
    }

    @Override
    protected OntologyModel loadModel(String ttlFile, String repoUrl) {
        return modelFactory.createOntology(ttlFile, repoUrl);
    }
}
