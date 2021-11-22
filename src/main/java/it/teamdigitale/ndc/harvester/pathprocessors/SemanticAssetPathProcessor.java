package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.SemanticAssetModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;

@RequiredArgsConstructor
@Slf4j
public abstract class SemanticAssetPathProcessor<P extends SemanticAssetPath, M extends SemanticAssetModel> {
    public void process(P path) {
        log.info("Processing path {}", path);

        log.debug("Loading model");
        M model = loadModel(path.getTtlPath());

        log.debug("Extracting main resource");
        Resource resource = model.getMainResource();
        log.info("Found resource {}", resource);

        processWithModel(path, model);
    }

    protected void processWithModel(P path, M model) {
        persistModelToTripleStore(model);

        indexMetadataForSearch(model);
    }

    protected void enrichModelBeforePersisting(M model) {
        // default implementation doesn't do anything.
        // if you need to enrich the RDF triples (e.g. add REST API URL property), you can override this method.
        // maybe call super() in there, anyways.
    }

    private void indexMetadataForSearch(M model) {
        log.debug("Indexing {} for search", model.getMainResource());

        // Note for #75 if this is actually common to all semantic types, it can stay here.
        // Maybe differences can be encapsulated in some method for extracting the SemanticAssetMetadata from
        // the SemanticAssetModel hierarchy, or maybe not. If that's not the case, of course we can make this (or
        // part of this) abstract and override accordingly.
    }

    private void persistModelToTripleStore(M model) {
        log.debug("Enriching model before persisting");
        enrichModelBeforePersisting(model);

        log.debug("Storing RDF content for {} in Virtuoso", model.getMainResource());
        // store triples in Virtuoso
    }

    protected abstract M loadModel(String ttlFile);
}
