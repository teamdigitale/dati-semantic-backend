package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.exception.SinglePathProcessingException;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import it.teamdigitale.ndc.harvester.pathprocessors.OntologyPathProcessor;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class HarvesterService {
    private final AgencyRepositoryService agencyRepositoryService;
    private final ControlledVocabularyPathProcessor controlledVocabularyPathProcessor;
    private final OntologyPathProcessor ontologyPathProcessor;
    private final TripleStoreRepository tripleStoreRepository;

    public void harvest(String repoUrl) throws IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl);
        tripleStoreRepository.clearExistingNamedGraph(repoUrl);
        harvestControlledVocabularies(repoUrl, path);
        harvestOntologies(repoUrl, path);
    }

    private void harvestOntologies(String repoUrl, Path rootPath) {
        List<SemanticAssetPath> ontologyPaths = agencyRepositoryService.getOntologyPaths(rootPath);
        for (SemanticAssetPath ontologyPath : ontologyPaths) {
            try {
                ontologyPathProcessor.process(repoUrl, ontologyPath);
            } catch (SinglePathProcessingException e) {
                log.error("Error processing ontology {} in repo {}", ontologyPath, repoUrl, e);
            }
        }
    }

    private void harvestControlledVocabularies(String repoUrl, Path rootPath) {
        List<CvPath> cvPaths = agencyRepositoryService.getControlledVocabularyPaths(rootPath);
        for (CvPath cvPath : cvPaths) {
            try {
                controlledVocabularyPathProcessor.process(repoUrl, cvPath);
            } catch (SinglePathProcessingException e) {
                log.error("Error processing controlled vocabulary {} in repo {}", cvPath, repoUrl, e);
            }
        }
    }
}
