package it.teamdigitale.ndc.harvester.model;

import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SemanticAssetModelFactory {
    private interface ModelConstructor<T extends SemanticAssetModel> {
        T build(Model model, String source);
    }

    public ControlledVocabularyModel createControlledVocabulary(String ttlFile) {
        return loadAndBuild(ttlFile, ControlledVocabularyModel::new);
    }

    public OntologyModel createOntology(String ttlFile) {
        return loadAndBuild(ttlFile, OntologyModel::new);
    }

    private <T extends SemanticAssetModel> T loadAndBuild(String source, ModelConstructor<T> c) {
        Model model = RDFDataMgr.loadModel(source, Lang.TURTLE);
        return c.build(model, source);
    }
}
