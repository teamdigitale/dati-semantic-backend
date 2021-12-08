package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.SchemaModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SchemaPathProcessor
    extends SemanticAssetPathProcessor<SemanticAssetPath, SchemaModel> {

    private final SemanticAssetModelFactory modelFactory;

    public SchemaPathProcessor(TripleStoreRepository tripleStoreRepository,
                               SemanticAssetModelFactory modelFactory,
                               SemanticAssetMetadataRepository metadataRepository) {
        super(tripleStoreRepository, metadataRepository);
        this.modelFactory = modelFactory;
    }

    @Override
    protected SchemaModel loadModel(String ttlFile, String repoUrl) {
        return modelFactory.createSchema(ttlFile, repoUrl);
    }
}
