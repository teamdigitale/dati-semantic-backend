package it.teamdigitale.ndc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.gen.dto.VocabularyData;
import it.teamdigitale.ndc.service.VocabularyDataService;
import it.teamdigitale.ndc.service.VocabularyIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        String agencyId = "agid";
        VocabularyData expected = mock(VocabularyData.class);
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        when(vocabularyDataService.getData(vocabId, pageable)).thenReturn(expected);

        VocabularyData actual = vocabularyDataController.fetchVocabularyData(agencyId, concept, offset, limit);

        assertEquals(expected, actual);
    }
}
