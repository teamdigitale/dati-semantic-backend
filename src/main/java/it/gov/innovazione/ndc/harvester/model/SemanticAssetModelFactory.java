package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.service.InstanceManager;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
public class SemanticAssetModelFactory {

    private final InstanceManager instanceManager;

    private interface ModelConstructor<T extends SemanticAssetModel> {
        T build(Model model, String source);
    }

    public ControlledVocabularyModel createControlledVocabulary(String ttlFile,
                                                                String repoUrl) {
        return loadAndBuild(ttlFile,
                (coreModel, source) -> new ControlledVocabularyModel(coreModel, source, repoUrl, instanceManager.getNextOnlineInstance(repoUrl)));
    }

    public OntologyModel createOntology(String ttlFile, String repoUrl) {
        return loadAndBuild(ttlFile,
                (coreModel, source) -> new OntologyModel(coreModel, source, repoUrl, instanceManager.getNextOnlineInstance(repoUrl)));
    }

    public SchemaModel createSchema(String ttlFile, String repoUrl) {
        return loadAndBuild(ttlFile,
                (coreModel, source) -> new SchemaModel(coreModel, source, repoUrl, instanceManager.getNextOnlineInstance(repoUrl)));
    }

    private <T extends SemanticAssetModel> T loadAndBuild(String source, ModelConstructor<T> c) {
        try {
            Model model = RDFDataMgr.loadModel(source, Lang.TURTLE);
            return c.build(model, source);
        } catch (Exception e) {
            throw new InvalidModelException(format("Cannot load RDF model from '%s'", source), e);
        }
    }
}
