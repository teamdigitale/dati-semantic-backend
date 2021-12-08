package it.teamdigitale.ndc.harvester.harvesters;

import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.SchemaPathProcessor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class SchemaHarvester extends BaseSemanticAssetHarvester<SemanticAssetPath> {
    private final AgencyRepositoryService agencyRepositoryService;
    private final SchemaPathProcessor pathProcessor;

    public SchemaHarvester(AgencyRepositoryService agencyRepositoryService, SchemaPathProcessor pathProcessor) {
        super(SemanticAssetType.SCHEMA);
        this.agencyRepositoryService = agencyRepositoryService;
        this.pathProcessor = pathProcessor;
    }

    @Override
    protected void processPath(String repoUrl, SemanticAssetPath path) {
        pathProcessor.process(repoUrl, path);
    }

    @Override
    protected List<SemanticAssetPath> scanForPaths(Path rootPath) {
        return agencyRepositoryService.getSchemaPaths(rootPath);
    }
}
