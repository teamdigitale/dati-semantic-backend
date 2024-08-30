package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static it.gov.innovazione.ndc.repository.TripleStoreRepository.ONLINE_GRAPH_PREFIX;
import static it.gov.innovazione.ndc.repository.TripleStoreRepository.TMP_GRAPH_PREFIX;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class HarvesterService {
    private final AgencyRepositoryService agencyRepositoryService;
    private final List<SemanticAssetHarvester> semanticAssetHarvesters;
    private final TripleStoreRepository tripleStoreRepository;
    private final SemanticAssetMetadataRepository semanticAssetMetadataRepository;
    private final RepositoryService repositoryService;
    private final FileUtils fileUtils;

    private static void updateContextWithMaintainers(List<Repository.Maintainer> maintainers) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (Objects.isNull(context)) {
            context = HarvestExecutionContext.builder()
                    .build();
        }
        context.addMaintainers(maintainers);
    }

    private static void updateContextWithRootPath(Path path) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (Objects.isNull(context)) {
            context = HarvestExecutionContext.builder()
                    .build();
        }
        HarvestExecutionContextUtils.setContext(context.withRootPath(path.toString()));
    }

    public void harvest(Repository repository) throws IOException {
        harvest(repository, null, Instance.PRIMARY);
    }

    public void harvest(Repository repository, String revision, Instance instance) throws IOException {
        log.info("Processing repo {}", repository.getUrl());
        Repository normalisedRepo = repository.withUrl(normaliseRepoUrl(repository.getUrl()));
        String repoUrl = normalisedRepo.getUrl();
        log.debug("Normalised repo url {}", repoUrl);
        try {
            Path path = cloneRepoToTempPath(repoUrl, revision);

            try {
                updateContext(path, instance);
                harvestClonedRepo(normalisedRepo, path);
            } finally {
                log.info("Cleaning up pending data for {}", path);
                agencyRepositoryService.removeClonedRepo(path);
                log.info("Cleaned up data for {} completed", path);
            }
            log.info("Repo {} processed correctly", repoUrl);
        } catch (IOException e) {
            log.error("Exception while processing {}", repoUrl, e);
            throw e;
        }
    }

    private void updateContext(Path path, Instance instance) {
        updateContextWithRootPath(path);
        updateContextWithMaintainers(fileUtils.getMaintainersIfPossible(path));
        updateContextWithInstance(instance);
    }

    private void updateContextWithInstance(Instance instance) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (Objects.isNull(context)) {
            context = HarvestExecutionContext.builder()
                    .build();
        }
        HarvestExecutionContextUtils.setContext(context.withInstance(instance));
    }

    public void clear(String repoUrl) {
        log.info("Clearing repo {}", repoUrl);
        repoUrl = normaliseRepoUrl(repoUrl);
        log.debug("Normalised repo url {}", repoUrl);
        try {
            clearRepoAllInstances(repoUrl);
            log.info("Repo {} cleared", repoUrl);
        } catch (Exception e) {
            log.error("Error while clearing {}", repoUrl, e);
            throw e;
        }
    }

    private String normaliseRepoUrl(String repoUrl) {
        return repoUrl.replace(".git", "");
    }

    private void harvestClonedRepo(Repository repository, Path path) {
        clearRepo(repository.getUrl(), HarvestExecutionContextUtils.getContext().getInstance());
        harvestSemanticAssets(repository, path);
        storeRightsHolders(repository);

        log.info("Repo {} processed", repository);
    }

    private void storeRightsHolders(Repository repository) {
        Map<String, Map<String, String>> rightsHolders = Optional.ofNullable(HarvestExecutionContextUtils.getContext())
                .map(HarvestExecutionContext::getRightsHolders)
                .orElse(Collections.emptyList()).stream()
                .collect(groupingBy(RightsHolder::getIdentifier, toList()))
                .entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().get(0).getName()));

        repositoryService.storeRightsHolders(repository, rightsHolders);
    }

    private void clearRepo(String repoUrl, Instance instance) {
        cleanUpWithHarvesters(repoUrl, instance);
        cleanUpTripleStore(repoUrl, TMP_GRAPH_PREFIX);
        cleanUpIndexedMetadata(repoUrl, instance);
    }

    private void clearRepoAllInstances(String repoUrl) {
        cleanUpWithHarvesters(repoUrl, Instance.PRIMARY);
        cleanUpWithHarvesters(repoUrl, Instance.SECONDARY);
        cleanUpTripleStore(repoUrl, TMP_GRAPH_PREFIX);
        cleanUpTripleStore(repoUrl, ONLINE_GRAPH_PREFIX);
        cleanUpIndexedMetadata(repoUrl, Instance.PRIMARY);
        cleanUpIndexedMetadata(repoUrl, Instance.SECONDARY);
    }

    private void cleanUpWithHarvesters(String repoUrl, Instance instance) {
        semanticAssetHarvesters.forEach(h -> {
            log.debug("Cleaning for {} before harvesting {}", h.getType(), repoUrl);
            h.cleanUpBeforeHarvesting(repoUrl, instance);

            log.debug("Cleaned for {}", h.getType());
        });
    }

    private void harvestSemanticAssets(Repository repository, Path path) {
        semanticAssetHarvesters.forEach(h -> {
            log.debug("Harvesting {} for {} assets", path, h.getType());
            h.harvest(repository, path);

            log.debug("Harvested {} for {} assets", path, h.getType());
        });
    }

    private Path cloneRepoToTempPath(String repoUrl, String revision) throws IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl, revision);
        log.debug("Repo {} cloned to temp folder {}", repoUrl, path);
        return path;
    }

    private void cleanUpTripleStore(String repoUrl, String prefix) {
        log.debug("Cleaning up triple store for {}", repoUrl);
        tripleStoreRepository.clearExistingNamedGraph(repoUrl, prefix);
    }

    private void cleanUpIndexedMetadata(String repoUrl, Instance instance) {
        log.debug("Cleaning up indexed metadata for {}", repoUrl);
        long deletedCount = semanticAssetMetadataRepository.deleteByRepoUrl(repoUrl, instance);
        log.debug("Deleted {} indexed metadata for {}", deletedCount, repoUrl);
    }

    public void cleanTempGraphsForConfiguredRepo() {
        log.info("Cleaning up temp graphs for configured repos");

        List<String> repoUrls = repositoryService.getActiveRepos().stream()
                .map(Repository::getUrl)
                .toList();

        repoUrls.forEach(tripleStoreRepository::clearTempGraphIfExists);

        log.info("Cleaning up temp graphs for configured repos completed");
    }

    public void clearTempGraphIfExists(String url) {
        tripleStoreRepository.clearTempGraphIfExists(url);
    }
}
