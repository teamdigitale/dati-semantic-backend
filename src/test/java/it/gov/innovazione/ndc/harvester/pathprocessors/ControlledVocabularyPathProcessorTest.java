package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.csv.CsvParser;
import it.gov.innovazione.ndc.harvester.csv.CsvParser.CsvData;
import it.gov.innovazione.ndc.harvester.csvapis.HarvestAssetStateService;
import it.gov.innovazione.ndc.harvester.csvapis.Sha256Hasher;
import it.gov.innovazione.ndc.harvester.model.ControlledVocabularyModel;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.harvester.model.validation.AssetValidationReport;
import it.gov.innovazione.ndc.harvester.model.validation.ValidationIssue;
import it.gov.innovazione.ndc.harvester.model.validation.ValidationIssueSeverity;
import it.gov.innovazione.ndc.harvester.model.validation.ValidationReport;
import it.gov.innovazione.ndc.harvester.service.SemanticContentStatsService;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidationResult;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import it.gov.innovazione.ndc.service.VocabularyDataService;
import it.gov.innovazione.ndc.service.VocabularyIdentifier;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyPathProcessorTest {
    public static final String REPO_URL = "my-repo.git";
    @Mock
    SemanticAssetModelFactory semanticAssetModelFactory;
    @Mock
    ControlledVocabularyModel cvModel;
    @Mock
    CsvParser csvParser;
    @Mock
    VocabularyDataService vocabularyDataService;
    @Mock
    TripleStoreRepository tripleStoreRepository;
    @Mock
    SemanticAssetMetadataRepository metadataRepository;
    @Mock
    SemanticContentStatsService semanticContentStatsService;
    @Mock
    RdfSyntaxValidator rdfSyntaxValidator;
    @Mock
    HarvestAssetStateService harvestAssetStateService;
    @Mock
    Model jenaModel;

    String baseUrl = "http://ndc";

    ControlledVocabularyPathProcessor pathProcessor;

    @BeforeEach
    void setup() {
        pathProcessor = new ControlledVocabularyPathProcessor(tripleStoreRepository, semanticAssetModelFactory,
                csvParser, vocabularyDataService, metadataRepository, rdfSyntaxValidator, harvestAssetStateService, baseUrl);
    }

    @Test
    void shouldProcessCsv() {
        String ttlFile = "cities.ttl";
        String csvFile = "cities.csv";
        CvPath path = CvPath.of(ttlFile, csvFile);

        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(RdfSyntaxValidationResult.builder().build());
        when(semanticAssetModelFactory.createControlledVocabulary(ttlFile, "some-repo")).thenReturn(cvModel);
        when(cvModel.getRdfModel()).thenReturn(jenaModel);
        when(cvModel.getKeyConcept()).thenReturn("keyConcept");
        when(cvModel.getAgencyId()).thenReturn(RightsHolder.builder().identifier("agencyId").build());
        CsvData csvData = new CsvData(List.of(Map.of("key", "val")), "key");
        when(csvParser.loadCsvDataFromFile(csvFile)).thenReturn(csvData);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(cvModel.extractMetadata()).thenReturn(metadata);

        pathProcessor.process("some-repo", path);

        verify(semanticAssetModelFactory).createControlledVocabulary(ttlFile, "some-repo");
        verify(csvParser).loadCsvDataFromFile(csvFile);
        verify(tripleStoreRepository).save("some-repo", jenaModel);
        verify(vocabularyDataService).indexData(new VocabularyIdentifier("agencyId", "keyConcept"),
                new CsvData(List.of(Map.of("key", "val")), "key"));
        verify(cvModel).extractMetadata();
        verify(metadataRepository).save(metadata);
    }

    @Test
    void shouldNotAttemptToProcessCsvIfOnlyTtlIsInPath() {
        String ttlFile = "cities.ttl";
        CvPath path = CvPath.of(ttlFile, null);

        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(RdfSyntaxValidationResult.builder().build());
        when(semanticAssetModelFactory.createControlledVocabulary(ttlFile, "some-repo")).thenReturn(cvModel);
        when(cvModel.getRdfModel()).thenReturn(jenaModel);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(cvModel.extractMetadata()).thenReturn(metadata);

        pathProcessor.process("some-repo", path);

        verify(semanticAssetModelFactory).createControlledVocabulary(ttlFile, "some-repo");
        verify(tripleStoreRepository).save("some-repo", jenaModel);
        verify(cvModel).extractMetadata();

        verify(cvModel, never()).getAgencyId();
        verify(cvModel, never()).getKeyConcept();
        verify(metadataRepository).save(metadata);
        verifyNoInteractions(csvParser);
        verifyNoInteractions(vocabularyDataService);
    }

    @Test
    void shouldAddNdcEndpointUrlToModelBeforePersisting() {
        pathProcessor.enrichModelBeforePersisting(cvModel, CvPath.of("cities.ttl", "cities.csv"));
        verify(cvModel).addNdcDataServiceProperties(baseUrl);
    }

    @Test
    void shouldNotAddNdcEndpointUrlToModelBeforePersistingWhenNoCsvIsPresent() {
        pathProcessor.enrichModelBeforePersisting(cvModel, CvPath.of("cities.ttl", null));
        verify(cvModel, never()).addNdcDataServiceProperties(baseUrl);
    }

    @Test
    void shouldDropIndicesForRepo() {
        String agencyId = "istat";
        String concept1 = "accomodation-ratings";
        String concept2 = "education-levels";
        List<SemanticAssetMetadata> vocabsMetadata = buildVocabsMetadataWithAgencyAndConcepts(agencyId, List.of(concept1, concept2));
        when(metadataRepository.findVocabulariesForRepoUrl(REPO_URL, Instance.PRIMARY)).thenReturn(vocabsMetadata);

        pathProcessor.dropCsvIndicesForRepo(REPO_URL, Instance.PRIMARY);

        verify(metadataRepository).findVocabulariesForRepoUrl(REPO_URL, Instance.PRIMARY);
        verify(vocabularyDataService).dropIndex(new VocabularyIdentifier(agencyId, concept1));
        verify(vocabularyDataService).dropIndex(new VocabularyIdentifier(agencyId, concept2));
    }

    @Test
    void shouldTryAndDropSubsequentIndicesEvenAfterFailingToDropOne() {
        String agencyId = "istat";
        String concept1 = "accomodation-ratings";
        String concept2 = "education-levels";
        List<SemanticAssetMetadata> vocabsMetadata = buildVocabsMetadataWithAgencyAndConcepts(agencyId, List.of(concept1, concept2));
        when(metadataRepository.findVocabulariesForRepoUrl(REPO_URL, Instance.PRIMARY)).thenReturn(vocabsMetadata);
        doThrow(new RuntimeException("Could not drop index")).when(vocabularyDataService).dropIndex(new VocabularyIdentifier(agencyId, concept1));

        pathProcessor.dropCsvIndicesForRepo(REPO_URL, Instance.PRIMARY);

        verify(vocabularyDataService).dropIndex(new VocabularyIdentifier(agencyId, concept1));
        verify(vocabularyDataService).dropIndex(new VocabularyIdentifier(agencyId, concept2));
    }

    private List<SemanticAssetMetadata> buildVocabsMetadataWithAgencyAndConcepts(String agencyId, List<String> keyConcepts) {
        SemanticAssetMetadata template = SemanticAssetMetadata.builder().repoUrl(REPO_URL).agencyId(agencyId).build();
        return keyConcepts.stream().map(c -> template.toBuilder().keyConcept(c).build()).collect(Collectors.toList());
    }

    @org.junit.jupiter.api.Nested
    class ApiStoreDbDiscovery {
        @TempDir
        Path tempDir;

        @BeforeEach
        void clearContextBefore() {
            HarvestExecutionContextUtils.clearContext();
        }

        @AfterEach
        void clearContextAfter() {
            HarvestExecutionContextUtils.clearContext();
        }

        @Test
        void shouldRegisterApiStoreDbWhenPresent() throws IOException {
            String ttlFile = tempDir.resolve("cv.ttl").toString();
            String csvFile = tempDir.resolve("cv.csv").toString();
            Path dbFile = tempDir.resolve("cv.db");
            Files.write(dbFile, new byte[]{0x42, 0x43, 0x44});
            String expectedHash = Sha256Hasher.hashFile(dbFile);

            CvPath path = CvPath.of(ttlFile, csvFile, dbFile.toString());
            String runId = "run-id";
            HarvestExecutionContextUtils.setContext(HarvestExecutionContext.builder()
                    .repository(Repository.builder().id("repo-id").url(REPO_URL).active(true).build())
                    .runId(runId)
                    .correlationId("corr-id")
                    .currentUserId("user")
                    .rootPath(tempDir.toString())
                    .instance(Instance.PRIMARY)
                    .build());

            when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(RdfSyntaxValidationResult.builder().build());
            when(semanticAssetModelFactory.createControlledVocabulary(ttlFile, REPO_URL)).thenReturn(cvModel);
            when(cvModel.getRdfModel()).thenReturn(jenaModel);
            when(cvModel.getKeyConcept()).thenReturn("keyConcept");
            when(cvModel.getAgencyId()).thenReturn(RightsHolder.builder().identifier("agencyId").build());
            when(csvParser.loadCsvDataFromFile(csvFile)).thenReturn(new CsvData(List.of(Map.of("key", "val")), "key"));
            when(cvModel.extractMetadata()).thenReturn(SemanticAssetMetadata.builder().build());

            pathProcessor.process(REPO_URL, path);

            verify(harvestAssetStateService).recordDbDetected(
                    eq(runId), eq(REPO_URL), eq("agencyId"), eq("keyConcept"),
                    eq(expectedHash), eq(dbFile), any(Instant.class));
        }

        @Test
        void shouldNotRegisterApiStoreDbWhenAbsent() {
            String ttlFile = "cv.ttl";
            CvPath path = CvPath.of(ttlFile, null);

            when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(RdfSyntaxValidationResult.builder().build());
            when(semanticAssetModelFactory.createControlledVocabulary(ttlFile, REPO_URL)).thenReturn(cvModel);
            when(cvModel.getRdfModel()).thenReturn(jenaModel);
            when(cvModel.extractMetadata()).thenReturn(SemanticAssetMetadata.builder().build());

            pathProcessor.process(REPO_URL, path);

            verifyNoInteractions(harvestAssetStateService);
        }

        @Test
        void shouldEmitImprovementIssueWhenDbIsMissing() throws IOException {
            String ttlFile = tempDir.resolve("cv.ttl").toString();
            CvPath path = CvPath.of(ttlFile, null);

            HarvestExecutionContextUtils.setContext(HarvestExecutionContext.builder()
                    .repository(Repository.builder().id("repo-id").url(REPO_URL).active(true).build())
                    .runId("run-id")
                    .correlationId("corr-id")
                    .currentUserId("user")
                    .rootPath(tempDir.toString())
                    .instance(Instance.PRIMARY)
                    .build());

            when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(RdfSyntaxValidationResult.builder().build());
            when(semanticAssetModelFactory.createControlledVocabulary(ttlFile, REPO_URL)).thenReturn(cvModel);
            when(cvModel.getRdfModel()).thenReturn(jenaModel);
            when(cvModel.extractMetadata()).thenReturn(SemanticAssetMetadata.builder().build());

            pathProcessor.process(REPO_URL, path);

            verifyNoInteractions(harvestAssetStateService);
            ValidationReport report = HarvestExecutionContextUtils.getContext()
                    .getValidationReportCollector().build(REPO_URL, "rev");
            AssetValidationReport assetReport = report.getAssetChecks().get(0);
            assertThat(assetReport.getAssetPath()).isEqualTo("cv.ttl");
            assertThat(assetReport.getIssues())
                    .extracting(ValidationIssue::getCode, ValidationIssue::getSeverity)
                    .contains(tuple("publish_api_workflow_missing", ValidationIssueSeverity.IMPROVEMENT));
        }

        @Test
        void shouldNotEmitIssueWhenDbIsPresent() throws IOException {
            Path dbFile = tempDir.resolve("cv.db");
            Files.write(dbFile, new byte[]{0x42});
            String ttlFile = tempDir.resolve("cv.ttl").toString();
            CvPath path = CvPath.of(ttlFile, null, dbFile.toString());

            HarvestExecutionContextUtils.setContext(HarvestExecutionContext.builder()
                    .repository(Repository.builder().id("repo-id").url(REPO_URL).active(true).build())
                    .runId("run-id")
                    .correlationId("corr-id")
                    .currentUserId("user")
                    .rootPath(tempDir.toString())
                    .instance(Instance.PRIMARY)
                    .build());

            when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(RdfSyntaxValidationResult.builder().build());
            when(semanticAssetModelFactory.createControlledVocabulary(ttlFile, REPO_URL)).thenReturn(cvModel);
            when(cvModel.getRdfModel()).thenReturn(jenaModel);
            when(cvModel.getKeyConcept()).thenReturn("kc");
            when(cvModel.getAgencyId()).thenReturn(RightsHolder.builder().identifier("ag").build());
            when(cvModel.extractMetadata()).thenReturn(SemanticAssetMetadata.builder().build());

            pathProcessor.process(REPO_URL, path);

            ValidationReport report = HarvestExecutionContextUtils.getContext()
                    .getValidationReportCollector().build(REPO_URL, "rev");
            assertThat(report.getAssetChecks().get(0).getIssues())
                    .extracting(ValidationIssue::getCode)
                    .doesNotContain("publish_api_workflow_missing");
        }

        @Test
        void shouldSwallowFailureWhenContextMissing() throws IOException {
            String ttlFile = tempDir.resolve("cv.ttl").toString();
            Path dbFile = tempDir.resolve("cv.db");
            Files.write(dbFile, new byte[]{0x01});

            CvPath path = CvPath.of(ttlFile, null, dbFile.toString());

            when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(RdfSyntaxValidationResult.builder().build());
            when(semanticAssetModelFactory.createControlledVocabulary(ttlFile, REPO_URL)).thenReturn(cvModel);
            when(cvModel.getRdfModel()).thenReturn(jenaModel);
            when(cvModel.extractMetadata()).thenReturn(SemanticAssetMetadata.builder().build());

            pathProcessor.process(REPO_URL, path);

            verifyNoInteractions(harvestAssetStateService);
        }
    }
}
