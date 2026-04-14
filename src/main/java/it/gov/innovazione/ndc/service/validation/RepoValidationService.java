package it.gov.innovazione.ndc.service.validation;

import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.BaseSemanticAssetModel;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.model.validation.ValidationReportCollector;
import it.gov.innovazione.ndc.harvester.service.RepositoryStructureValidator;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidationResult;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepoValidationService {

    private static final int MAX_CONCURRENT_VALIDATIONS = 3;
    private final Semaphore concurrencyLimiter = new Semaphore(MAX_CONCURRENT_VALIDATIONS);

    private final AgencyRepositoryService agencyRepositoryService;
    private final RdfSyntaxValidator rdfSyntaxValidator;
    private final RepositoryStructureValidator repositoryStructureValidator;
    private final SemanticAssetModelFactory modelFactory;

    /**
     * Attempt to acquire a validation permit. Must be called before
     * {@link #executeValidation}. The caller is responsible for calling
     * {@link #releaseSemaphore()} if the job is NOT submitted after a
     * successful acquire (e.g. on unexpected error between acquire and submit).
     * When {@code executeValidation} runs, it always releases the permit
     * in its {@code finally} block.
     */
    public boolean tryAcquire() {
        return concurrencyLimiter.tryAcquire();
    }

    public void releaseSemaphore() {
        concurrencyLimiter.release();
    }

    @Async
    public void executeValidation(ValidationJob job) {
        Path clonedPath = null;
        try {
            job.setStatus(ValidationJob.Status.CLONING);
            String repoUrl = job.getRepoUrl();
            clonedPath = agencyRepositoryService.cloneRepoWithoutTracking(repoUrl, job.getRevision());

            ValidationReportCollector collector = new ValidationReportCollector();

            job.setStatus(ValidationJob.Status.DISCOVERING);

            List<DiscoveredAsset> allAssets = discoverAssets(clonedPath);
            job.setTotalAssets(allAssets.size());

            repositoryStructureValidator.validate(clonedPath)
                    .ifPresent(collector::addRepositoryChecks);

            job.setStatus(ValidationJob.Status.VALIDATING);

            for (DiscoveredAsset asset : allAssets) {
                try {
                    String relativePath = relativize(clonedPath, asset.path.getTtlPath());
                    validateAsset(collector, asset, repoUrl, relativePath);
                } catch (Exception e) {
                    log.warn("Error validating asset {}: {}", asset.path.getTtlPath(), e.getMessage());
                }
                job.getProcessedAssets().incrementAndGet();
            }

            job.setReport(collector.build(repoUrl, job.getRevision()));
            job.setCompletedAt(Instant.now());
            job.setStatus(ValidationJob.Status.COMPLETED);
            log.info("Validation completed for {}/{}: {} assets",
                    job.getOwner(), job.getRepo(), allAssets.size());

        } catch (Exception e) {
            log.error("Validation failed for {}/{}: {}", job.getOwner(), job.getRepo(), e.getMessage(), e);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            job.setStatus(ValidationJob.Status.FAILED);
        } finally {
            concurrencyLimiter.release();
            if (clonedPath != null) {
                try {
                    agencyRepositoryService.removeClonedRepo(clonedPath);
                } catch (Exception e) {
                    log.warn("Failed to cleanup cloned repo at {}", clonedPath, e);
                }
            }
        }
    }

    private void validateAsset(ValidationReportCollector collector, DiscoveredAsset asset,
                                String repoUrl, String relativePath) {
        RdfSyntaxValidationResult syntaxResult =
                rdfSyntaxValidator.validateTurtle(asset.path.getTtlPath());
        collector.addSyntaxResult(relativePath, asset.type, syntaxResult);

        if (syntaxResult.hasErrors()) {
            collector.markAssetSkipped(relativePath);
            return;
        }

        try {
            BaseSemanticAssetModel model = createModelForValidation(asset, repoUrl);
            SemanticAssetModelValidationContext ctx = model.validateMetadata();
            collector.addMetadataResult(relativePath, ctx);
        } catch (Exception e) {
            log.debug("Cannot load model for {}: {}", relativePath, e.getMessage());
        }
    }

    private BaseSemanticAssetModel createModelForValidation(DiscoveredAsset asset, String repoUrl) {
        String ttlFile = asset.path.getTtlPath();
        return switch (asset.type) {
            case ONTOLOGY -> modelFactory.createOntologyForValidation(ttlFile, repoUrl);
            case CONTROLLED_VOCABULARY -> modelFactory.createControlledVocabularyForValidation(ttlFile, repoUrl);
            case SCHEMA -> modelFactory.createSchemaForValidation(ttlFile, repoUrl);
        };
    }

    private List<DiscoveredAsset> discoverAssets(Path clonedPath) {
        List<DiscoveredAsset> assets = new ArrayList<>();

        for (SemanticAssetPath p : agencyRepositoryService.getOntologyPaths(clonedPath)) {
            assets.add(new DiscoveredAsset(p, SemanticAssetType.ONTOLOGY));
        }
        for (CvPath p : agencyRepositoryService.getControlledVocabularyPaths(clonedPath)) {
            assets.add(new DiscoveredAsset(p, SemanticAssetType.CONTROLLED_VOCABULARY));
        }
        for (SemanticAssetPath p : agencyRepositoryService.getSchemaPaths(clonedPath)) {
            assets.add(new DiscoveredAsset(p, SemanticAssetType.SCHEMA));
        }

        return assets;
    }

    private static String relativize(Path root, String absolutePath) {
        return absolutePath.replace(root.toString() + "/", "");
    }

    private record DiscoveredAsset(SemanticAssetPath path, SemanticAssetType type) {
    }
}
