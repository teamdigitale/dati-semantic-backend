package it.gov.innovazione.ndc.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.gov.innovazione.ndc.controller.exception.VocabularyDataNotFoundException;
import it.gov.innovazione.ndc.controller.exception.VocabularyItemNotFoundException;
import it.gov.innovazione.ndc.gen.dto.VocabulariesResult;
import it.gov.innovazione.ndc.gen.dto.VocabularySummary;
import it.gov.innovazione.ndc.model.Builders;
import it.gov.innovazione.ndc.service.SemanticAssetSearchService;
import it.gov.innovazione.ndc.service.VocabularyDataService;
import it.gov.innovazione.ndc.service.VocabularyIdentifier;
import it.gov.innovazione.ndc.gen.dto.VocabularyData;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VocabularyDataController.class)
public class VocabularyDataControllerMvcTest {
    @MockBean
    private VocabularyDataService vocabularyDataService;
    @MockBean
    private SemanticAssetSearchService assetSearchService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnControlledVocabularyItems() throws Exception {
        String agencyId = "agid";
        String concept = "person-title";
        final int offset = 1;
        final int limit = 2;
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        VocabularyData expectedVocabularyData = Builders.vocabularyData()
                .limit(limit)
                .offset(offset)
                .totalResults(10)
                .data(List.of(Map.of("title", "Mr"), Map.of("title", "Mrs")))
                .build();
        when(vocabularyDataService.getData(eq(vocabId), any(OffsetBasedPageRequest.class))).thenReturn(expectedVocabularyData);

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
    void shouldReturnControlledVocabularies() throws Exception {
        final int offset = 1;
        final int limit = 2;
        VocabularySummary summary1 = Builders.vocabularySummary()
                .title("Some vocab")
                .description("Its description")
                .agencyId("agid")
                .keyConcept("interesting-stuff")
                .endpointUrl("http://www.schema.gov.it/vocabs/agid/interesting-stuff")
                .build();
        VocabularySummary summary2 = Builders.vocabularySummary()
                .title("Some other vocab")
                .description("Its description")
                .agencyId("istat")
                .keyConcept("stimulating-stuff")
                .endpointUrl("http://www.schema.gov.it/vocabs/istat/stimulating-stuff")
                .build();
        VocabulariesResult expectedResult = Builders.vocabulariesResult()
                .limit(limit)
                .offset(offset)
                .totalCount(10)
                .data(List.of(summary1, summary2))
                .build();
        when(assetSearchService.getVocabularies(any(OffsetBasedPageRequest.class))).thenReturn(expectedResult);

        mockMvc.perform(get("/vocabularies")
                        .param("offset", Integer.toString(offset))
                        .param("limit", Integer.toString(limit))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.limit").value(limit))
                .andExpect(jsonPath("$.offset").value(offset))
                .andExpect(jsonPath("$.totalCount").value(10))
                .andExpect(jsonPath("$.data[0].title").value("Some vocab"))
                .andExpect(jsonPath("$.data[0].links[0].rel").value("items"))
                .andExpect(jsonPath("$.data[0].links[0].href").value("http://www.schema.gov.it/vocabs/agid/interesting-stuff"))
                .andExpect(jsonPath("$.data[1].title").value("Some other vocab"));
    }

    @Test
    void shouldReturn404WhenVocabularyNotFound() throws Exception {
        String concept = "missing";
        final int offset = 1;
        final int limit = 2;
        String agencyId = "acme";
        VocabularyIdentifier vocabId = new VocabularyIdentifier(agencyId, concept);
        when(vocabularyDataService.getData(eq(vocabId), any(OffsetBasedPageRequest.class))).thenThrow(new VocabularyDataNotFoundException("acme.missing"));

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

    @Test
    void shouldReturnVocabularyDataUsingDefaultPagination() throws Exception {
        when(vocabularyDataService.getData(any(), any()))
                .thenReturn(Builders.vocabularyData()
                        .offset(1)
                        .limit(10)
                        .totalResults(5)
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
    void shouldReturnNotFoundForWholeVocabulary() throws Exception {
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

    @ParameterizedTest
    @ValueSource(strings = {"/vocabularies/agid/testKeyConcept", "/vocabularies"})
    void shouldFailWhenOffsetIsLessThan0(String url) throws Exception {
        mockMvc.perform(get(url).param("offset", "-1"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vocabularyDataService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/vocabularies/agid/testKeyConcept", "/vocabularies"})
    void shouldFailWhenLimitIsLessThan1(String url) throws Exception {
        mockMvc.perform(get(url).param("limit", "0"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()))
                .andExpect(jsonPath("$.title", containsString("limit")));

        verifyNoInteractions(vocabularyDataService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/vocabularies/agid/testKeyConcept", "/vocabularies"})
    void shouldFailWhenPageSizeIsMoreThan200(String url) throws Exception {
        mockMvc.perform(get(url).param("limit", "201"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()))
                .andExpect(jsonPath("$.title", containsString("limit")));

        verifyNoInteractions(vocabularyDataService);
    }
}
