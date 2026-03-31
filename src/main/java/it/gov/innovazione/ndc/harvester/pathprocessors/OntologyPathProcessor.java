package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.OntologyModel;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OntologyPathProcessor extends BaseSemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {
    private final SemanticAssetModelFactory modelFactory;

    public OntologyPathProcessor(TripleStoreRepository tripleStoreRepository, SemanticAssetModelFactory modelFactory,
                                 SemanticAssetMetadataRepository metadataRepository,
                                 RdfSyntaxValidator rdfSyntaxValidator) {
        super(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        this.modelFactory = modelFactory;
    }

    @Override
    protected OntologyModel loadModel(String ttlFile, String repoUrl) {
        return modelFactory.createOntology(ttlFile, repoUrl);
    }

    @Override
    protected SemanticAssetType getAssetType() {
        return SemanticAssetType.ONTOLOGY;
    }
}
