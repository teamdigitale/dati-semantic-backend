package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class ControlledVocabularyHarvester extends BaseSemanticAssetHarvester<CvPath> {
    private final AgencyRepositoryService agencyRepositoryService;
    private final ControlledVocabularyPathProcessor pathProcessor;

    public ControlledVocabularyHarvester(AgencyRepositoryService agencyRepositoryService, ControlledVocabularyPathProcessor pathProcessor) {
        super(SemanticAssetType.CONTROLLED_VOCABULARY);
        this.agencyRepositoryService = agencyRepositoryService;
        this.pathProcessor = pathProcessor;
    }

    @Override
    protected void processPath(String repoUrl, CvPath path) {
        pathProcessor.process(repoUrl, path);
    }

    @Override
    protected List<CvPath> scanForPaths(Path rootPath) {
        return agencyRepositoryService.getControlledVocabularyPaths(rootPath);
    }

    @Override
    public void cleanUpBeforeHarvesting(String repoUrl) {
        pathProcessor.dropCsvIndicesForRepo(repoUrl);
    }
}
