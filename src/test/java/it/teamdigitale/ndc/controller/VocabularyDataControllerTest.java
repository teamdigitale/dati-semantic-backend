package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.controller.exception.VocabularyDataNotFoundException;
import it.teamdigitale.ndc.controller.exception.VocabularyItemNotFoundException;
import it.teamdigitale.ndc.gen.dto.VocabularyData;
import it.teamdigitale.ndc.model.Builders;
import it.teamdigitale.ndc.service.VocabularyDataService;
import it.teamdigitale.ndc.service.VocabularyIdentifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VocabularyDataController.class)
public class VocabularyDataControllerTest {
    @MockBean
    private VocabularyDataService vocabularyDataService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnControlledVocabularyItems() throws Exception {
        String concept = "person-title";
        final int offset = 1;
        final int limit = 2;
        String agencyId = "agid";
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        VocabularyData expectedVocabularyData = Builders.vocabularyData()
                .limit(limit)
                .offset(offset)
                .totalResults(10)
                .data(List.of(Map.of("title", "Mr"), Map.of("title", "Mrs")))
                .build();
        when(vocabularyDataService.getData(vocabId, pageable)).thenReturn(expectedVocabularyData);

        mockMvc.perform(get("/vocabularies/agid/person-title")
                        .param("offset", Integer.toString(offset))
                        .param("limit", Integer.toString(limit))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.limit").value(limit))
                .andExpect(jsonPath("$.offset").value(offset))
                .andExpect(jsonPath("$.totalResults").value(10))
                .andExpect(jsonPath("$.data[0].title").value("Mr"));
    }

    @Test
    void shouldReturnControlledVocabularyItem() throws Exception {
        String concept = "person-title";
        String agencyId = "agid";
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        when(vocabularyDataService.getItem(vocabId, "1")).thenReturn(Map.of("id", "1", "title", "Mr"));

        mockMvc.perform(get("/vocabularies/agid/person-title/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.title").value("Mr"));
    }

    @Test
    void shouldReturn404WhenVocabularyNotFound() throws Exception {
        String concept = "missing";
        final int offset = 1;
        final int limit = 2;
        String agencyId = "acme";
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);
        when(vocabularyDataService.getData(vocabId, pageable)).thenThrow(new VocabularyDataNotFoundException("acme.missing"));

        mockMvc.perform(get("/vocabularies/acme/missing")
                        .param("offset", Integer.toString(offset))
                        .param("limit", Integer.toString(limit))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));
    }

    @Test
    void shouldReturn404WhenVocabularyForItemNotFound() throws Exception {
        String concept = "missing";
        String agencyId = "acme";
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        when(vocabularyDataService.getItem(vocabId, "123")).thenThrow(new VocabularyDataNotFoundException("acme.missing"));

        mockMvc.perform(get("/vocabularies/acme/missing/123")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));
    }

    @Test
    void shouldReturn404WhenVocabularyItemNotFound() throws Exception {
        String concept = "person-title";
        String agencyId = "agid";
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        when(vocabularyDataService.getItem(vocabId, "456")).thenThrow(new VocabularyItemNotFoundException("agid.person-title", "456"));

        mockMvc.perform(get("/vocabularies/agid/person-title/456")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));
    }
}
