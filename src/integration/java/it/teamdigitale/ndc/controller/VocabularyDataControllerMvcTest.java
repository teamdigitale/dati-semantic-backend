package it.teamdigitale.ndc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.teamdigitale.ndc.controller.exception.VocabularyDataNotFoundException;
import it.teamdigitale.ndc.controller.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VocabularyDataController.class)
public class VocabularyDataControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VocabularyDataService vocabularyDataService;

    @Test
    public void shouldReturnVocabularyDataUsingDefaultPagination() throws Exception {
        when(vocabularyDataService.getData(any(), any(), any(), any()))
            .thenReturn(VocabularyDataDto.builder()
                .pageNumber(1)
                .totalResults(5L)
                .data(List.of(Map.of("key", "val")))
                .build());

        mockMvc.perform(get("/vocabularies/agid/testKeyConcept"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNumber").value(1))
            .andExpect(jsonPath("$.totalResults").value(5))
            .andExpect(jsonPath("$.data[0].key").value("val"));

        verify(vocabularyDataService).getData("agid", "testKeyConcept",
            0, 10);
    }

    @Test
    public void shouldReturnNotFound() throws Exception {
        when(vocabularyDataService.getData(any(), any(), any(), any()))
            .thenThrow(new VocabularyDataNotFoundException("agid.testkeyconcept"));

        mockMvc.perform(get("/vocabularies/agid/testKeyConcept"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(
                "Unable to find vocabulary data for : agid.testkeyconcept"));

        verify(vocabularyDataService).getData("agid", "testKeyConcept",
            0, 10);
    }

    @Test
    void shouldFailWhenPageNumberIsLessThan1() throws Exception {
        mockMvc.perform(get("/vocabularies/agid/testKeyConcept")
                .param("page_number", "0"))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verifyNoInteractions(vocabularyDataService);
    }

    @Test
    void shouldFailWhenPageSizeIsLessThan1() throws Exception {
        mockMvc.perform(get("/vocabularies/agid/testKeyConcept")
                .param("page_size", "0"))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verifyNoInteractions(vocabularyDataService);
    }

    @Test
    void shouldFailWhenPageSizeIsMoreThan200() throws Exception {
        mockMvc.perform(get("/vocabularies/agid/testKeyConcept")
                .param("page_size", "201"))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verifyNoInteractions(vocabularyDataService);
    }
}
