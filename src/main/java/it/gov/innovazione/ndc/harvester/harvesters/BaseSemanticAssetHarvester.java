package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.config.HarvestExecutionContext;
import it.gov.innovazione.ndc.config.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.eventhandler.event.HarvestedFileTooBigEvent;
import it.gov.innovazione.ndc.eventhandler.event.HarvestedFileTooBigEvent.ViolatingSemanticAsset;
import it.gov.innovazione.ndc.harvester.SemanticAssetHarvester;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.exception.SinglePathProcessingException;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseSemanticAssetHarvester<P extends SemanticAssetPath> implements SemanticAssetHarvester {
    private final SemanticAssetType type;
    private final NdcEventPublisher eventPublisher;

    @Override
    public SemanticAssetType getType() {
        return type;
    }

    @Override
    public void harvest(Repository repository, Path rootPath) {
        log.debug("Looking for {} paths", type);

        List<P> paths = scanForPaths(rootPath);

        paths.forEach(this::notifyIfSizeExceed);

        log.debug("Found {} {} path(s) for processing", paths.size(), type);

        for (P path : paths) {
            try {
                processPath(repository.getUrl(), path);
                log.debug("Path {} processed correctly for {}", path, type);

            } catch (SinglePathProcessingException e) {
                log.error("Error processing {} {} in repo {}", type, path, repository.getUrl(), e);
            }
        }
    }

    private void notifyIfSizeExceed(P path) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (context != null) {
            List<File> files = path.getAllFiles();
            if (isBiggerThan(context.getRepository().getMaxFileSizeBytes(), files)) {
                notify(context, path);
            }
        }
    }

    private void notify(HarvestExecutionContext context, P path) {
        eventPublisher.publishEvent(
                "harvester",
                "harvester.max-file-size-exceeded",
                context.getCorrelationId(),
                context.getCurrentUserId(),
                HarvestedFileTooBigEvent.builder()
                        .runId(context.getRunId())
                        .repository(context.getRepository())
                        .revision(context.getRevision())
                        .violatingSemanticAsset(
                                ViolatingSemanticAsset.fromPath(
                                        relativize(
                                                path.getTtlPath(),
                                                context),
                                        path.getAllFiles(),
                                        context.getRepository().getMaxFileSizeBytes()))
                        .build());
        log.warn("File {} is bigger than maxFileSizeBytes", path.getTtlPath());
    }

    private String relativize(String ttlFile, HarvestExecutionContext context) {
        return getFolderNameFromFile(ttlFile).replace(
                context.getRootPath(), "");
    }

    private String getFolderNameFromFile(String ttlFile) {
        return new File(ttlFile).getParent();
    }

    private boolean isBiggerThan(Long maxFileSizeBytes, List<File> files) {
        return maxFileSizeBytes != null
               && maxFileSizeBytes > 0
               && files.stream().anyMatch(file -> file.length() > maxFileSizeBytes);
    }

    @Override
    public void cleanUpBeforeHarvesting(String repoUrl) {
        // by default nothing specific
    }

    protected abstract void processPath(String repoUrl, P path);

    protected abstract List<P> scanForPaths(Path rootPath);
}
