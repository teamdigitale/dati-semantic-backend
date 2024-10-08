package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.DefaultAlertableEvent;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.eventhandler.event.HarvestedFileTooBigEvent;
import it.gov.innovazione.ndc.eventhandler.event.HarvestedFileTooBigEvent.ViolatingSemanticAsset;
import it.gov.innovazione.ndc.harvester.SemanticAssetHarvester;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.exception.SinglePathProcessingException;
import it.gov.innovazione.ndc.harvester.harvesters.utils.PathUtils;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.ConfigKey.MAX_FILE_SIZE_BYTES;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseSemanticAssetHarvester<P extends SemanticAssetPath> implements SemanticAssetHarvester {

    private static final List<String> INFRASTRUCTURE_EXCEPTIONS = List.of("java.net", "org.apache.http");

    private final SemanticAssetType type;
    private final NdcEventPublisher eventPublisher;
    private final ConfigService configService;

    @Override
    public SemanticAssetType getType() {
        return type;
    }

    private static boolean isInfrastructureTypeException(Throwable cause) {
        return INFRASTRUCTURE_EXCEPTIONS.stream().anyMatch(cause.getClass().getName()::contains);
    }

    @Override
    public void harvest(Repository repository, Path rootPath) {
        log.debug("Looking for {} paths", type);

        List<P> paths = scanForPaths(rootPath);

        Long maxFileSizeBytes = configService.getFromRepoOrGlobalOrDefault(
                MAX_FILE_SIZE_BYTES, repository.getId(), 0L);

        log.debug("Found {} {} path(s) for processing", paths.size(), type);

        paths.forEach(p -> notifyIfSizeExceed(p, maxFileSizeBytes));

        for (P path : paths) {
            try {
                processPath(repository.getUrl(), path);
                log.debug("Path {} processed correctly for {}", path, type);
            } catch (SinglePathProcessingException e) {
                boolean isInfrastuctureError = checkInfrastructureError(e);
                Optional.ofNullable(HarvestExecutionContextUtils.getContext())
                        .ifPresent(context -> context.addHarvestingError(repository, e, path.getAllFiles()));
                eventPublisher.publishAlertableEvent(
                        "harvester",
                        DefaultAlertableEvent.builder()
                                .name("Harvester Single Path Processing Error")
                                .description("Error processing " + type + " " + path + " in repo " + repository.getUrl())
                                .category(isInfrastuctureError ? EventCategory.INFRASTRUCTURE : EventCategory.SEMANTIC)
                                .severity(Severity.ERROR)
                                .context(Map.of(
                                        "error", e.getRealErrorMessage(),
                                        "path", path.getTtlPath(),
                                        "repo", repository.getUrl(),
                                        "isFatal", e.isFatal()))
                                .build());
                log.error("Error processing {} {} in repo {}", type, path, repository.getUrl(), e);
                if (e.isFatal()) {
                    throw e;
                }
            }
        }
    }

    private boolean checkInfrastructureError(SinglePathProcessingException e) {
        // checks if in the chain of exceptions there is an infrastructure error (es. java.net, httpException, etc)
        Throwable cause = e;
        Set<Throwable> seen = new HashSet<>();
        while (cause != null) {
            if (!seen.add(cause)) {
                return false;
            }
            if (isInfrastructureTypeException(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void notifyIfSizeExceed(P path, Long maxFileSizeBytes) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (Objects.nonNull(context) && Objects.nonNull(path)) {
            List<File> files = path.getAllFiles();

            if (Objects.nonNull(maxFileSizeBytes) && maxFileSizeBytes > 0 && isAnyBiggerThan(maxFileSizeBytes, files)) {
                files.stream()
                        .filter(file -> file.length() > maxFileSizeBytes)
                        .forEach(file -> log.info("[FILE-SCANNER] -- File(s) {} is bigger than {} ", file.getName(), maxFileSizeBytes));
                notify(context, path, maxFileSizeBytes);
            }
        }
    }

    private void notify(HarvestExecutionContext context, P path, Long maxFileSizeBytes) {
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
                                        PathUtils.relativizeFolder(
                                                path.getTtlPath(),
                                                context),
                                        path.getAllFiles(),
                                        maxFileSizeBytes))
                        .build());
        log.warn("File {} is bigger than maxFileSizeBytes={}", path.getTtlPath(), maxFileSizeBytes);
    }

    private boolean isAnyBiggerThan(Long maxFileSizeBytes, List<File> files) {
        return maxFileSizeBytes != null
               && maxFileSizeBytes > 0
               && files.stream().anyMatch(file -> file.length() > maxFileSizeBytes);
    }

    @Override
    public void cleanUpBeforeHarvesting(String repoUrl, Instance instance) {
        // by default nothing specific
    }

    protected abstract void processPath(String repoUrl, P path);

    protected abstract List<P> scanForPaths(Path rootPath);
}
