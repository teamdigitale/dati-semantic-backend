package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
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

    public void harvest(String repoUrl) throws IOException {
        log.info("Processing repo {}", repoUrl);
        repoUrl = normaliseRepoUrl(repoUrl);
        log.debug("Normalised repo url {}", repoUrl);
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

    private String normaliseRepoUrl(String repoUrl) {
        return repoUrl.replace(".git", "");
    }

    private void harvestClonedRepo(String repoUrl, Path path) {
        cleanUpTripleStore(repoUrl);
        cleanUpIndexedMetadata(repoUrl);

        harvestSemanticAssets(repoUrl, path);

        log.info("Repo {} processed", repoUrl);
    }

    private void harvestSemanticAssets(String repoUrl, Path path) {
        semanticAssetHarvesters.forEach(h -> h.harvest(repoUrl, path));
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
        long deletedCount = semanticAssetMetadataRepository.deleteByRepoUrl(repoUrl);
        log.debug("Deleted {} indexed metadata for {}", deletedCount, repoUrl);
    }
}
