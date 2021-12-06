package it.teamdigitale.ndc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.controller.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class VocabularyDataControllerTest {
    @Mock
    VocabularyDataService vocabularyDataService;
    @InjectMocks
    VocabularyDataController vocabularyDataController;

    @Test
    void shouldReturnControlledVocabulary() {
        String concept = "person-title";
        final int offset = 1;
        final int limit = 2;
        String rightsHolder = "agid";
        VocabularyDataDto expected = mock(VocabularyDataDto.class);
        when(vocabularyDataService.getData(rightsHolder, concept, OffsetBasedPageRequest.of(offset, limit))).thenReturn(expected);

        VocabularyDataDto actual = vocabularyDataController.fetchVocabularyData(rightsHolder, concept, offset, limit);

        assertEquals(expected, actual);
    }
}
