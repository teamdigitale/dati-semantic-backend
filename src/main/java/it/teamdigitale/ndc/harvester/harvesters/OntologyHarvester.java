package it.teamdigitale.ndc.harvester.harvesters;

import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.OntologyPathProcessor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class OntologyHarvester extends BaseSemanticAssetHarvester<SemanticAssetPath> {
    private final AgencyRepositoryService agencyRepositoryService;
    private final OntologyPathProcessor pathProcessor;

    public OntologyHarvester(AgencyRepositoryService agencyRepositoryService, OntologyPathProcessor pathProcessor) {
        super(SemanticAssetType.ONTOLOGY);
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
