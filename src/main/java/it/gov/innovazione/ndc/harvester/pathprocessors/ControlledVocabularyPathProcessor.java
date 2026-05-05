package it.gov.innovazione.ndc.harvester.pathprocessors;

import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticWarn;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.csv.CsvParser;
import it.gov.innovazione.ndc.harvester.csv.CsvParser.CsvData;
import it.gov.innovazione.ndc.harvester.csvapis.HarvestAssetStateService;
import it.gov.innovazione.ndc.harvester.csvapis.Sha256Hasher;
import it.gov.innovazione.ndc.harvester.model.ControlledVocabularyModel;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.model.HarvesterStatsHolder;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import it.gov.innovazione.ndc.service.VocabularyDataService;
import it.gov.innovazione.ndc.service.VocabularyIdentifier;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ControlledVocabularyPathProcessor extends BaseSemanticAssetPathProcessor<CvPath, ControlledVocabularyModel> {
    private final SemanticAssetModelFactory modelFactory;
    private final CsvParser csvParser;
    private final VocabularyDataService vocabularyDataService;
    private final HarvestAssetStateService harvestAssetStateService;
    private final String baseUrl;

    public ControlledVocabularyPathProcessor(TripleStoreRepository tripleStoreRepository, SemanticAssetModelFactory modelFactory,
                                             CsvParser csvParser, VocabularyDataService vocabularyDataService,
                                             SemanticAssetMetadataRepository metadataRepository,
                                             RdfSyntaxValidator rdfSyntaxValidator,
                                             HarvestAssetStateService harvestAssetStateService,
                                             @Value("${ndc.baseUrl}") String baseUrl) {
        super(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        this.modelFactory = modelFactory;
        this.csvParser = csvParser;
        this.vocabularyDataService = vocabularyDataService;
        this.harvestAssetStateService = harvestAssetStateService;
        this.baseUrl = baseUrl;
    }

    @Override
    protected HarvesterStatsHolder processWithModel(String repoUrl, CvPath path, ControlledVocabularyModel model) {
        HarvesterStatsHolder harvesterStatsHolder = super.processWithModel(repoUrl, path, model);

        path.getCsvPath().ifPresent(p -> {
            String keyConcept = model.getKeyConcept();
            String agencyId = model.getAgencyId().getIdentifier();
            VocabularyIdentifier vocabularyIdentifier = new VocabularyIdentifier(agencyId, keyConcept);

            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .message("Indexing CSV for " + vocabularyIdentifier)
                    .additionalInfo("csvPath", p)
                    .additionalInfo("vocabularyIdentifier", vocabularyIdentifier)
                    .additionalInfo("keyConcept", keyConcept)
                    .additionalInfo("agencyId", agencyId)
                    .build());

            parseAndIndexCsv(vocabularyIdentifier, p);
        });

        path.getDbPath().ifPresent(dbPath -> recordApiStoreDbDiscovery(repoUrl, dbPath, model));

        return harvesterStatsHolder;
    }

    @Override
    protected ControlledVocabularyModel loadModel(String ttlFile, String repoUrl) {
        return modelFactory.createControlledVocabulary(ttlFile, repoUrl);
    }

    @Override
    protected SemanticAssetModelValidationContext validateMetadataForReport(String ttlFile, String repoUrl) {
        return modelFactory.createControlledVocabularyForValidation(ttlFile, repoUrl).validateMetadata();
    }

    @Override
    protected void enrichModelBeforePersisting(ControlledVocabularyModel model, CvPath path) {
        path.getCsvPath().ifPresent(p -> model.addNdcDataServiceProperties(baseUrl));
    }

    private void parseAndIndexCsv(VocabularyIdentifier vocabularyIdentifier, String csvPath) {
        CsvData flatData = csvParser.loadCsvDataFromFile(csvPath);
        vocabularyDataService.indexData(vocabularyIdentifier, flatData);
    }

    private void recordApiStoreDbDiscovery(String repoUrl, String dbPath, ControlledVocabularyModel model) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (context == null) {
            log.warn("No harvest execution context available; skipping APIStore .db registration for {}", dbPath);
            return;
        }
        try {
            String runId = context.getRunId();
            String agencyId = model.getAgencyId().getIdentifier();
            String keyConcept = model.getKeyConcept();
            String hash = Sha256Hasher.hashFile(Path.of(dbPath));
            harvestAssetStateService.recordDbDetected(runId, repoUrl, agencyId, keyConcept, hash, Instant.now());
            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .message("Registered APIStore .db for " + agencyId + "/" + keyConcept)
                    .additionalInfo("dbPath", dbPath)
                    .additionalInfo("sourceDbHash", hash)
                    .additionalInfo("agencyId", agencyId)
                    .additionalInfo("keyConcept", keyConcept)
                    .build());
        } catch (Exception e) {
            log.error("Failed to register APIStore .db at {}", dbPath, e);
            logSemanticWarn(LoggingContext.builder()
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .message("Failed to register APIStore .db for " + dbPath)
                    .details(e.getMessage())
                    .additionalInfo("dbPath", dbPath)
                    .build());
        }
    }

    public void dropCsvIndicesForRepo(String repoUrl, Instance instance) {
        log.debug("Retrieving vocab metadata for {} to drop indices", repoUrl);

        List<SemanticAssetMetadata> vocabs = metadataRepository.findVocabulariesForRepoUrl(repoUrl, instance);

        if (log.isDebugEnabled()) {
            log.debug("Found {} vocabs with indices to drop", vocabs.size());
        }

        if (vocabs.isEmpty()) {
            return;
        }

        logSemanticInfo(LoggingContext.builder()
                .stage(HarvesterStage.CLEANING_METADATA)
                .message("Cleaning " + vocabs.size() + " found vocabularies")
                .additionalInfo("vocabs", vocabs.stream().map(SemanticAssetMetadata::getIri).collect(Collectors.joining(",")))
                .build());

        vocabs.forEach(v -> {
            VocabularyIdentifier vocabId = new VocabularyIdentifier(v.getAgencyId(), v.getKeyConcept());

            tryToDropIndex(v, vocabId);
        });
    }

    @Override
    protected SemanticAssetType getAssetType() {
        return SemanticAssetType.CONTROLLED_VOCABULARY;
    }

    private void tryToDropIndex(SemanticAssetMetadata v, VocabularyIdentifier vocabId) {
        log.info("Dropping {} for {}", vocabId, v.getIri());
        try {
            vocabularyDataService.dropIndex(vocabId);
            log.info("{} dropped", vocabId);
        } catch (Exception e) {
            logSemanticError(LoggingContext.builder()
                    .stage(HarvesterStage.CLEANING_METADATA)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .message("Could not drop index " + vocabId)
                    .additionalInfo("vocabId", vocabId)
                    .additionalInfo("iri", v.getIri())
                    .build());
            log.error("Could not drop index {}", vocabId, e);
        }
    }
}
