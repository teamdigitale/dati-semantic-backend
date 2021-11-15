package it.teamdigitale.ndc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
public class VocabularyDataControllerTest {
    @Mock
    VocabularyDataService vocabularyDataService;
    @InjectMocks
    VocabularyDataController vocabularyDataController;

    @Test
    void shouldReturnControlledVocabulary() {
        String indexName = "person-title";
        final int pageNumber = 1;
        final int pageSize = 2;
        VocabularyDataDto expected = mock(VocabularyDataDto.class);
        when(vocabularyDataService.getData(indexName, pageNumber - 1, pageSize)).thenReturn(expected);

        VocabularyDataDto actual = vocabularyDataController.fetchVocabularyData("agid",
                indexName,
                pageNumber,
                pageSize);

        assertEquals(expected, actual);
    }
}
