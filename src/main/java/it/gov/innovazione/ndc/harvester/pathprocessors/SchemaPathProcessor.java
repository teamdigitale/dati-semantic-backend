package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.model.SchemaModel;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SchemaPathProcessor
    extends BaseSemanticAssetPathProcessor<SemanticAssetPath, SchemaModel> {

    private final SemanticAssetModelFactory modelFactory;

    public SchemaPathProcessor(TripleStoreRepository tripleStoreRepository,
                               SemanticAssetModelFactory modelFactory,
                               SemanticAssetMetadataRepository metadataRepository,
                               RdfSyntaxValidator rdfSyntaxValidator) {
        super(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        this.modelFactory = modelFactory;
    }

    @Override
    protected SchemaModel loadModel(String ttlFile, String repoUrl) {
        return modelFactory.createSchema(ttlFile, repoUrl);
    }
}
