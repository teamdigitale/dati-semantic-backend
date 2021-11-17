package it.teamdigitale.ndc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        String indexName = "person-title";
        final int pageNumber = 1;
        final int pageSize = 2;
        String agencyId = "agid";
        VocabularyDataDto expected = mock(VocabularyDataDto.class);
        when(vocabularyDataService.getData(agencyId, indexName, pageNumber - 1, pageSize)).thenReturn(expected);

        VocabularyDataDto actual = vocabularyDataController.fetchVocabularyData(agencyId,
                indexName,
                pageNumber,
                pageSize);

        assertEquals(expected, actual);
    }
}
