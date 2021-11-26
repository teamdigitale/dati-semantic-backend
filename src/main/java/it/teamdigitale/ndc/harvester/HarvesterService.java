package it.teamdigitale.ndc.harvester;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;

import it.teamdigitale.ndc.harvester.exception.SinglePathProcessingException;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import it.teamdigitale.ndc.harvester.pathprocessors.OntologyPathProcessor;
import it.teamdigitale.ndc.harvester.pathprocessors.SemanticAssetPathProcessor;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HarvesterService {
    private final AgencyRepositoryService agencyRepositoryService;
    private final ControlledVocabularyPathProcessor controlledVocabularyPathProcessor;
    private final OntologyPathProcessor ontologyPathProcessor;
    private final TripleStoreRepository tripleStoreRepository;
    private final SemanticAssetMetadataRepository semanticAssetMetadataRepository;

    public void harvest(String repoUrl) throws IOException {
        log.info("Processing repo {}", repoUrl);
        try {
            Path path = cloneRepoToTempPath(repoUrl);

            try {
                harvestClonedRepo(repoUrl, path);
            } finally {
                agencyRepositoryService.removeClonedRepo(path);
            }
        } catch (IOException e) {
            log.error("Exception while processing {}", repoUrl, e);
            throw e;
        }
    }

    private void harvestClonedRepo(String repoUrl, Path path) {
        cleanUpTripleStore(repoUrl);
        cleanUpIndexedMetadata(repoUrl);

        harvestControlledVocabularies(repoUrl, path);
        harvestOntologies(repoUrl, path);

        log.info("Repo {} processed", repoUrl);
    }

    private Path cloneRepoToTempPath(String repoUrl) throws IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl);
        log.debug("Repo {} cloned to temp folder {}", repoUrl, path);
        return path;
    }

    private void cleanUpTripleStore(String repoUrl) {
        log.debug("Cleaning up triple store for {}", repoUrl);
        tripleStoreRepository.clearExistingNamedGraph(repoUrl);
    }

    private void cleanUpIndexedMetadata(String repoUrl) {
        log.debug("Cleaning up indexed metadata for {}", repoUrl);
        semanticAssetMetadataRepository.deleteByRepoUrl(repoUrl);
    }

    private void harvestOntologies(String repoUrl, Path rootPath) {
        harvestAssetsOfType(ONTOLOGY, repoUrl, rootPath,
            agencyRepositoryService::getOntologyPaths,
            ontologyPathProcessor);
    }

    private void harvestControlledVocabularies(String repoUrl, Path rootPath) {
        harvestAssetsOfType(CONTROLLED_VOCABULARY, repoUrl, rootPath,
            agencyRepositoryService::getControlledVocabularyPaths,
            controlledVocabularyPathProcessor);
    }

    private interface PathSupplier<P extends SemanticAssetPath> {
        List<P> get(Path rootPath);
    }

    private <P extends SemanticAssetPath, M extends SemanticAssetModel> void harvestAssetsOfType(
        SemanticAssetType type, String repoUrl, Path rootPath,
        PathSupplier<P> pathSupplier,
        SemanticAssetPathProcessor<P, M> pathProcessor) {
        log.debug("Looking for {} paths", type);

        List<P> paths = pathSupplier.get(rootPath);
        log.debug("Found {} {} path(s) for processing", paths.size(), type);

        for (P path : paths) {
            try {
                pathProcessor.process(repoUrl, path);
            } catch (SinglePathProcessingException e) {
                log.error("Error processing {} {} in repo {}", type, path, repoUrl, e);
            }
        }
    }


}
