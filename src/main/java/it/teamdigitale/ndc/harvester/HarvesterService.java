package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
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

    public void harvest(String repoUrl) throws IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl);
        harvestControlledVocabularies(path);
        harvestOntologies(path);
    }

    private void harvestOntologies(Path path) {
        List<SemanticAssetPath> ontologyPaths = agencyRepositoryService.getOntologyPaths(path);
        for (SemanticAssetPath ontologyPath : ontologyPaths) {
            processOntologyPath(ontologyPath);
        }
    }

    private void processOntologyPath(SemanticAssetPath ontologyPath) {
        log.info("parsing and loading file {}", ontologyPath.getTtlPath());
        // load the ttl file into memory

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }

    private void harvestControlledVocabularies(Path rootPath) {
        List<CvPath> cvPaths = agencyRepositoryService.getControlledVocabularyPaths(rootPath);
        for (CvPath cvPath : cvPaths) {
            controlledVocabularyPathProcessor.process(cvPath);
        }
    }
}
