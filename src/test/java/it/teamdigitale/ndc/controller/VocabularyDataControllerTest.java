package it.teamdigitale.ndc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.service.VocabularyDataService;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VocabularyDataControllerTest {
    @Mock
    VocabularyDataService vocabularyDataService;
    @InjectMocks
    VocabularyDataController vocabularyDataController;

    @Test
    void shouldReturnControlledVocabulary() {
        String indexName = "person-title";
        List expected = Arrays.asList(mock(JSONObject.class));
        when(vocabularyDataService.getData(indexName)).thenReturn(expected);
        List<JSONObject> actual = vocabularyDataController.fetchControlledVocabularyData("agid", indexName);

        assertEquals(expected, actual);
    }
}
