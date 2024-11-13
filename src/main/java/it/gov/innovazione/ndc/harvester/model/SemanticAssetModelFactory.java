package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.InstanceManager;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Component;

import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
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
            logSemanticInfo(
                    LoggingContext.builder()
                            .message(format("Loaded RDF model from '%s'", source))
                            .additionalInfo("modelSize", model.size())
                            .additionalInfo("modelSource", source)
                            .harvesterStatus(HarvesterRun.Status.RUNNING)
                            .stage(HarvesterStage.PROCESS_RESOURCE)
                            .build());
            return c.build(model, source);
        } catch (Exception e) {
            logSemanticError(
                    LoggingContext.builder()
                            .message(format("Cannot load RDF model from '%s'", source))
                            .harvesterStatus(HarvesterRun.Status.RUNNING)
                            .details(e.getMessage())
                            .stage(HarvesterStage.PROCESS_RESOURCE)
                            .build());
            throw new InvalidModelException(format("Cannot load RDF model from '%s'", source), e);
        }
    }
}
