package it.teamdigitale.ndc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
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
        String concept = "person-title";
        final int pageNumber = 1;
        final int pageSize = 2;
        String rightsHolder = "agid";
        VocabularyDataDto expected = mock(VocabularyDataDto.class);
        when(vocabularyDataService.getData(rightsHolder, concept, pageNumber - 1,
            pageSize)).thenReturn(expected);

        VocabularyDataDto actual = vocabularyDataController.fetchVocabularyData(rightsHolder,
            concept,
            pageNumber,
            pageSize);

        assertEquals(expected, actual);
    }
}
