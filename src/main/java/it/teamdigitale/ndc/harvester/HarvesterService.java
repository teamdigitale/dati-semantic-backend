package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import it.teamdigitale.ndc.harvester.pathprocessors.OntologyPathProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HarvesterService {
    private final AgencyRepositoryService agencyRepositoryService;
    private final ControlledVocabularyPathProcessor controlledVocabularyPathProcessor;
    private final OntologyPathProcessor ontologyPathProcessor;

    public void harvest(String repoUrl) throws IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl);
        harvestControlledVocabularies(path);
        harvestOntologies(path);
    }

    private void harvestOntologies(Path path) {
        List<SemanticAssetPath> ontologyPaths = agencyRepositoryService.getOntologyPaths(path);
        for (SemanticAssetPath ontologyPath : ontologyPaths) {
            ontologyPathProcessor.process(ontologyPath);
        }
    }

    private void harvestControlledVocabularies(Path rootPath) {
        List<CvPath> cvPaths = agencyRepositoryService.getControlledVocabularyPaths(rootPath);
        for (CvPath cvPath : cvPaths) {
            controlledVocabularyPathProcessor.process(cvPath);
        }
    }
}
