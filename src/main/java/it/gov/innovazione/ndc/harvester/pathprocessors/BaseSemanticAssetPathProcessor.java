package it.gov.innovazione.ndc.harvester.pathprocessors;

import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logInfrastructureError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticWarn;
import static java.util.stream.Collectors.toList;

import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.exception.SinglePathProcessingException;
import it.gov.innovazione.ndc.harvester.model.HarvesterStatsHolder;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModel;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.model.extractors.RightsHolderExtractor;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;

@RequiredArgsConstructor
@Slf4j
public abstract class BaseSemanticAssetPathProcessor<P extends SemanticAssetPath, M extends SemanticAssetModel> implements SemanticAssetPathProcessor<P> {
    private final TripleStoreRepository tripleStoreRepository;
    protected final SemanticAssetMetadataRepository metadataRepository;

    private static <M extends SemanticAssetModel> SemanticAssetMetadata tryExtractMetadata(M model) {
        try {
            return model.extractMetadata();
        } catch (Exception e) {
            log.error("Error extracting metadata for {}", model.getMainResource(), e);
            throw new SinglePathProcessingException("Cannot extract metadata", e, false);
        }
    }

    protected HarvesterStatsHolder processWithModel(String repoUrl, P path, M model) {
        log.debug("Enriching model before persisting");
        enrichModelBeforePersisting(model, path);
        SemanticAssetModelValidationContext.ValidationContextStats statsBefore = getStats(model);
        SemanticAssetMetadata meta = indexMetadataForSearch(model);
        SemanticAssetModelValidationContext.ValidationContextStats statsAfter = getStats(model);
        persistModelToTripleStore(repoUrl, model);
        collectRightsHolderInContext(repoUrl, model);
        return HarvesterStatsHolder.builder()
                .metadata(meta)
                .validationContextStats(SemanticAssetModelValidationContext.difference(statsBefore, statsAfter))
                .build();
    }

    private static <M extends SemanticAssetModel> SemanticAssetModelValidationContext.ValidationContextStats getStats(M model) {
        return Optional.ofNullable(model)
                .map(SemanticAssetModel::getValidationContext)
                .map(SemanticAssetModelValidationContext.ValidationContextStats::of)
                .orElse(SemanticAssetModelValidationContext.ValidationContextStats.empty());
    }

    private void collectRightsHolderInContext(String repoUrl, M model) {
        try {
            HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
            context.addRightsHolder(RightsHolderExtractor.getAgencyId(model.getMainResource(), NO_VALIDATION));
            logSemanticInfo(LoggingContext.builder()
                    .message("Added rights holder to context")
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("rightsHolder", context.getRightsHolders())
                    .build());
        } catch (Exception e) {
            log.error("Error adding rights holder to repo " + repoUrl, e);
            logSemanticWarn(LoggingContext.builder()
                    .message("Error adding rights holder to context")
                    .details(e.getMessage())
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .build());
        }
    }

    protected void enrichModelBeforePersisting(M model, P path) {
        // default implementation doesn't do anything.
        // if you need to enrich the RDF triples (e.g. add REST API URL property), you can override this method.
        // maybe call super() in there, anyways.
    }

    @Override
    public HarvesterStatsHolder process(String repoUrl, P path) {
        try {
            log.info("Processing path {}", path);

            log.debug("Loading model from {}", path);
            M model = loadModel(path.getTtlPath(), repoUrl);

            Resource resource = model.getMainResource();
            log.info("Found resource {}", resource);

            HarvesterStatsHolder harvesterStatsHolder = processWithModel(repoUrl, path, model);
            log.info("Path {} processed", path);

            return harvesterStatsHolder;
        } catch (Exception e) {
            log.error("Error processing {}", path, e);
            if (e instanceof SinglePathProcessingException singlePathProcessingException) {
                logSemanticError(LoggingContext.builder()
                        .message("Error processing " + path)
                        .details(singlePathProcessingException.getRealErrorMessage())
                        .stage(HarvesterStage.PROCESS_RESOURCE)
                        .harvesterStatus(HarvesterRun.Status.RUNNING)
                        .build());
                throw new SinglePathProcessingException(String.format("Cannot process '%s'", path), e, singlePathProcessingException.isFatal());
            }
            logInfrastructureError(LoggingContext.builder()
                    .message("Error processing " + path)
                    .details(e.getMessage())
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .build());
            throw new SinglePathProcessingException(String.format("Cannot process '%s'", path), e, false);
        }
    }

    private SemanticAssetMetadata indexMetadataForSearch(M model) {
        log.debug("Indexing {} for search", model.getMainResource());
        SemanticAssetMetadata metadata = tryExtractMetadata(model);
        postProcessMetadata(metadata);
        try {
            metadataRepository.save(metadata);
            logSemanticInfo(LoggingContext.builder()
                    .message("Indexed metadata for " + model.getMainResource())
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("metadata", metadata)
                    .build());
            return metadata;
        } catch (Exception e) {
            log.error("Error saving metadata for {}", model.getMainResource(), e);
            throw new SinglePathProcessingException("Cannot save metadata", e, true);
        }
    }

    protected void postProcessMetadata(SemanticAssetMetadata metadata) {
        if (Objects.nonNull(metadata)) {
            metadata.setKeyClassesLabels(
                    Optional.of(metadata)
                            .map(SemanticAssetMetadata::getKeyClasses)
                            .orElse(Collections.emptyList()).stream()
                            .map(NodeSummary::getSummary)
                            .toList());
        }
    }

    private void persistModelToTripleStore(String repoUrl, M model) {
        log.debug("Storing RDF content for {} in Virtuoso", model.getMainResource());
        try {
            tripleStoreRepository.save(repoUrl, model.getRdfModel());
            logSemanticInfo(LoggingContext.builder()
                    .message("Saved RDF content for " + model.getMainResource())
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .build());
        } catch (Exception e) {
            log.error("Error saving RDF content for {}", model.getMainResource(), e);
            throw new SinglePathProcessingException("Cannot save RDF content", e, true);
        }
    }

    protected abstract M loadModel(String ttlFile, String repoUrl);
}
