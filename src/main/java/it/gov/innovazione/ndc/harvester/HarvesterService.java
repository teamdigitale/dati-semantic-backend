package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
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
    private final List<SemanticAssetHarvester> semanticAssetHarvesters;
    private final TripleStoreRepository tripleStoreRepository;
    private final SemanticAssetMetadataRepository semanticAssetMetadataRepository;

    public void harvest(Repository repository) throws IOException {
        harvest(repository, null);
    }

    public void harvest(Repository repository, String revision) throws IOException {
        log.info("Processing repo {}", repository.getUrl());
        String repoUrl = normaliseRepoUrl(repository.getUrl());
        log.debug("Normalised repo url {}", repoUrl);
        try {
            Path path = cloneRepoToTempPath(repoUrl, revision);

            try {
                harvestClonedRepo(repoUrl, path);
            } finally {
                agencyRepositoryService.removeClonedRepo(path);
            }
            log.info("Repo {} processed correctly", repoUrl);
        } catch (IOException e) {
            log.error("Exception while processing {}", repoUrl, e);
            throw e;
        }
    }

    public void clear(String repoUrl) {
        log.info("Clearing repo {}", repoUrl);
        repoUrl = normaliseRepoUrl(repoUrl);
        log.debug("Normalised repo url {}", repoUrl);
        try {
            clearRepo(repoUrl);
            log.info("Repo {} cleared", repoUrl);
        } catch (Exception e) {
            log.error("Error while clearing {}", repoUrl, e);
            throw e;
        }
    }

    private String normaliseRepoUrl(String repoUrl) {
        return repoUrl.replace(".git", "");
    }

    private void harvestClonedRepo(String repoUrl, Path path) {
        clearRepo(repoUrl);

        harvestSemanticAssets(repoUrl, path);

        log.info("Repo {} processed", repoUrl);
    }

    private void clearRepo(String repoUrl) {
        cleanUpWithHarvesters(repoUrl);
        cleanUpTripleStore(repoUrl);
        cleanUpIndexedMetadata(repoUrl);
    }

    private void cleanUpWithHarvesters(String repoUrl) {
        semanticAssetHarvesters.forEach(h -> {
            log.debug("Cleaning for {} before harvesting {}", h.getType(), repoUrl);
            h.cleanUpBeforeHarvesting(repoUrl);

            log.debug("Cleaned for {}", h.getType());
        });
    }

    private void harvestSemanticAssets(String repoUrl, Path path) {
        semanticAssetHarvesters.forEach(h -> {
            log.debug("Harvesting {} for {} assets", path, h.getType());
            h.harvest(repoUrl, path);

            log.debug("Harvested {} for {} assets", path, h.getType());
        });
    }

    private Path cloneRepoToTempPath(String repoUrl, String revision) throws IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl, revision);
        log.debug("Repo {} cloned to temp folder {}", repoUrl, path);
        return path;
    }

    private void cleanUpTripleStore(String repoUrl) {
        log.debug("Cleaning up triple store for {}", repoUrl);
        tripleStoreRepository.clearExistingNamedGraph(repoUrl);
    }

    private void cleanUpIndexedMetadata(String repoUrl) {
        log.debug("Cleaning up indexed metadata for {}", repoUrl);
        long deletedCount = semanticAssetMetadataRepository.deleteByRepoUrl(repoUrl);
        log.debug("Deleted {} indexed metadata for {}", deletedCount, repoUrl);
    }
}
