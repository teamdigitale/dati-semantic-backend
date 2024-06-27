package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.pathprocessors.OntologyPathProcessor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class OntologyHarvester extends BaseSemanticAssetHarvester<SemanticAssetPath> {
    private final AgencyRepositoryService agencyRepositoryService;
    private final OntologyPathProcessor pathProcessor;

    public OntologyHarvester(AgencyRepositoryService agencyRepositoryService, OntologyPathProcessor pathProcessor, NdcEventPublisher ndcEventPublisher, ConfigService configService) {
        super(SemanticAssetType.ONTOLOGY, ndcEventPublisher, configService);
        this.agencyRepositoryService = agencyRepositoryService;
        this.pathProcessor = pathProcessor;
    }

    @Override
    protected void processPath(String repoUrl, SemanticAssetPath path) {
        pathProcessor.process(repoUrl, path);
    }

    @Override
    protected List<SemanticAssetPath> scanForPaths(Path rootPath) {
        return agencyRepositoryService.getOntologyPaths(rootPath);
    }
}
