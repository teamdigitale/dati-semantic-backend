package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.service.VocabularyDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyPathProcessorTest {
    @Mock
    SemanticAssetModelFactory semanticAssetModelFactory;
    @Mock
    ControlledVocabularyModel cvModel;
    @Mock
    CsvParser csvParser;
    @Mock
    VocabularyDataService vocabularyDataService;
    @Mock
    TripleStoreRepository repository;
    @InjectMocks
    ControlledVocabularyPathProcessor pathProcessor;

    @Test
    void shouldProcessCsv() {
        String ttlFile = "cities.ttl";
        String csvFile = "cities.csv";
        CvPath path = CvPath.of(ttlFile, csvFile);

        when(semanticAssetModelFactory.createControlledVocabulary(ttlFile)).thenReturn(cvModel);
        when(cvModel.getKeyConcept()).thenReturn("keyConcept");
        when(cvModel.getRightsHolderId()).thenReturn("rightsHolderId");
        when(csvParser.convertCsvToMapList(csvFile)).thenReturn(List.of(Map.of("key", "val")));

        pathProcessor.process("some-repo", path);

        verify(semanticAssetModelFactory).createControlledVocabulary(ttlFile);
        verify(csvParser).convertCsvToMapList(csvFile);
        verify(vocabularyDataService).indexData("rightsHolderId", "keyConcept", List.of(Map.of("key", "val")));
    }
}