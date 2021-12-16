package it.teamdigitale.ndc.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.teamdigitale.ndc.controller.exception.VocabularyDataNotFoundException;
import it.teamdigitale.ndc.model.ModelBuilder;
import it.teamdigitale.ndc.service.VocabularyDataService;
import java.util.List;
import java.util.Map;

import it.teamdigitale.ndc.service.VocabularyIdentifier;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VocabularyDataController.class)
public class VocabularyDataControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VocabularyDataService vocabularyDataService;

    @Test
    public void shouldReturnVocabularyDataUsingDefaultPagination() throws Exception {
        when(vocabularyDataService.getData(any(), any()))
            .thenReturn(ModelBuilder.vocabularyDataBuilder()
                .offset(1L)
                .limit(10)
                .totalResults(5L)
                .data(List.of(Map.of("key", "val")))
                .build());

        mockMvc.perform(get("/vocabularies/agid/testKeyConcept"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offset").value(1))
            .andExpect(jsonPath("$.limit").value(10))
            .andExpect(jsonPath("$.totalResults").value(5))
            .andExpect(jsonPath("$.data[0].key").value("val"));

        verify(vocabularyDataService).getData(
                new VocabularyIdentifier("agid", "testKeyConcept"), OffsetBasedPageRequest.of(0, 10));
    }

    @Test
    public void shouldReturnNotFoundForWholeVocabulary() throws Exception {
        when(vocabularyDataService.getData(any(), any()))
            .thenThrow(new VocabularyDataNotFoundException("agid.testkeyconcept"));

        mockMvc.perform(get("/vocabularies/agid/testKeyConcept"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()))
            .andExpect(jsonPath("$.title").value(
                "Unable to find vocabulary data for : agid.testkeyconcept"));

        verify(vocabularyDataService).getData(
                new VocabularyIdentifier("agid", "testKeyConcept"), OffsetBasedPageRequest.of(0, 10));
    }

    @Test
    void shouldFailWhenOffsetIsLessThan0() throws Exception {
        mockMvc.perform(get("/vocabularies/agid/testKeyConcept")
                .param("offset", "-1"))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verifyNoInteractions(vocabularyDataService);
    }

    @Test
    void shouldFailWhenLimitIsLessThan1() throws Exception {
        mockMvc.perform(get("/vocabularies/agid/testKeyConcept")
                .param("limit", "0"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()))
            .andExpect(jsonPath("$.title", containsString("limit")));

        verifyNoInteractions(vocabularyDataService);
    }

    @Test
    void shouldFailWhenPageSizeIsMoreThan200() throws Exception {
        mockMvc.perform(get("/vocabularies/agid/testKeyConcept")
                .param("limit", "201"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()))
            .andExpect(jsonPath("$.title", containsString("limit")));

        verifyNoInteractions(vocabularyDataService);
    }
}
