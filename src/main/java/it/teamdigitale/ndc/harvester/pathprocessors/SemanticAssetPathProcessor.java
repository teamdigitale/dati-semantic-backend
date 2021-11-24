package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.exception.SinglePathProcessingException;
import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepositoryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;

@RequiredArgsConstructor
@Slf4j
public abstract class SemanticAssetPathProcessor<P extends SemanticAssetPath, M extends SemanticAssetModel> {
    private final TripleStoreRepository tripleStoreRepository;
    private final SemanticAssetMetadataRepository metadataRepository;

    public void process(String repoUrl, P path) {
        log.info("Processing path {}", path);

        log.debug("Loading model");
        M model = loadModel(path.getTtlPath());

        log.debug("Extracting main resource");
        Resource resource = model.getMainResource();
        log.info("Found resource {}", resource);

        processWithModel(repoUrl, path, model);
        log.info("Path {} processed", path);
    }

    protected void processWithModel(String repoUrl, P path, M model) {
        persistModelToTripleStore(repoUrl, model);

        indexMetadataForSearch(model);
    }

    protected void enrichModelBeforePersisting(M model) {
        // default implementation doesn't do anything.
        // if you need to enrich the RDF triples (e.g. add REST API URL property), you can override this method.
        // maybe call super() in there, anyways.
    }

    private void indexMetadataForSearch(M model) {
        log.debug("Indexing {} for search", model.getMainResource());
        SemanticAssetMetadata metadata = model.extractMetadata();
        metadataRepository.save(metadata);
    }

    private void persistModelToTripleStore(String repoUrl, M model) {
        log.debug("Enriching model before persisting");
        enrichModelBeforePersisting(model);

        try {
            log.debug("Storing RDF content for {} in Virtuoso", model.getMainResource());
            tripleStoreRepository.save(repoUrl, model.getRdfModel());
        } catch (TripleStoreRepositoryException e) {
            log.error("Failed to persist model to triple store", e);
            throw new SinglePathProcessingException("Could not persist model to triple store", e);
        }
    }

    protected abstract M loadModel(String ttlFile);
}
