package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.csv.CsvParser;
import it.gov.innovazione.ndc.harvester.csv.CsvParser.CsvData;
import it.gov.innovazione.ndc.harvester.model.ControlledVocabularyModel;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import it.gov.innovazione.ndc.service.VocabularyDataService;
import it.gov.innovazione.ndc.service.VocabularyIdentifier;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    Model jenaModel;

    String baseUrl = "http://ndc";

    ControlledVocabularyPathProcessor pathProcessor;

    @BeforeEach
    void setup() {
        pathProcessor = new ControlledVocabularyPathProcessor(tripleStoreRepository, semanticAssetModelFactory,
                csvParser, vocabularyDataService, metadataRepository, baseUrl);
    }

    @Test
    void shouldProcessCsv() {
        String ttlFile = "cities.ttl";
        String csvFile = "cities.csv";
        CvPath path = CvPath.of(ttlFile, csvFile);

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
}
