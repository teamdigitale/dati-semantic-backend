package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.controller.exception.SemanticAssetNotFoundException;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.model.Builders;
import it.gov.innovazione.ndc.service.SemanticAssetSearchService;
import it.gov.innovazione.ndc.gen.dto.SearchResultItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SemanticAssetsController.class)
public class SemanticAssetsControllerMvcTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private SemanticAssetSearchService searchService;

    @Test
    void shouldFindByIri() throws Exception {
        SemanticAssetDetails dto = new SemanticAssetDetails();
        dto.setAssetIri("some-iri");
        dto.setTitle("some-title");
        dto.setDescription("some-description");
        when(searchService.findByIri("some-iri")).thenReturn(dto);

        mockMvc.perform(get("/semantic-assets/by-iri").param("iri", "some-iri"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.assetIri").value("some-iri"))
                .andExpect(jsonPath("$.title").value("some-title"))
                .andExpect(jsonPath("$.description").value("some-description"));
    }

    @Test
    void shouldReturn404WhenSemanticAssetNotFound() throws Exception {
        when(searchService.findByIri("some-iri")).thenThrow(
                new SemanticAssetNotFoundException("some-iri"));

        mockMvc.perform(get("/semantic-assets/by-iri")
                        .param("iri", "some-iri")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()))
                .andExpect(jsonPath("$.title").value("Semantic Asset not found for Iri : some-iri"));
    }

    @Test
    void shouldReturnMatchingAssetsUsingDefaultPageParams() throws Exception {
        SearchResultItem dto = new SearchResultItem();
        dto.setAssetIri("some-iri");
        dto.setDescription("some-description");
        dto.setModifiedOn(LocalDate.parse("2020-01-01"));

        when(searchService.search(any(), any(), any(), any())
        ).thenReturn(Builders.searchResult()
                .limit(10)
                .offset(0)
                .totalCount(1)
                .data(List.of(dto))
                .build());

        ResultActions apiResult = mockMvc.perform(get("/semantic-assets")
                .param("q", "searchText")
                .param("type", "CONTROLLED_VOCABULARY")
                .param("type", "ONTOLOGY")
                .param("theme", "http://publications.europa.eu/resource/authority/data-theme/AGRI")
                .param("theme", "http://publications.europa.eu/resource/authority/data-theme/EDUC")
                .accept(MediaType.APPLICATION_JSON)
        );

        apiResult
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.data[0].assetIri").value("some-iri"))
                .andExpect(jsonPath("$.data[0].description").value("some-description"))
                .andExpect(jsonPath("$.data[0].modifiedOn").value("2020-01-01"));

        verify(searchService).search("searchText",
                Set.of("CONTROLLED_VOCABULARY", "ONTOLOGY"),
                Set.of("http://publications.europa.eu/resource/authority/data-theme/AGRI", "http://publications.europa.eu/resource/authority/data-theme/EDUC"),
                OffsetBasedPageRequest.of(0, 10));
    }

    @Test
    void shouldReturnMatchingAssetsUsingProvidedPageParams() throws Exception {
        SearchResultItem dto = new SearchResultItem();
        when(searchService.search(any(), any(), any(), any())
        ).thenReturn(Builders.searchResult()
                .limit(20)
                .offset(100)
                .totalCount(101)
                .data(List.of(dto))
                .build());

        ResultActions apiResult = mockMvc.perform(get("/semantic-assets")
                .param("limit", "20")
                .param("offset", "100")
                .accept(MediaType.APPLICATION_JSON)
        );

        verify(searchService).search("", Set.of(), Set.of(), OffsetBasedPageRequest.of(100, 20));

        apiResult
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalCount").value(101))
                .andExpect(jsonPath("$.limit").value(20))
                .andExpect(jsonPath("$.offset").value(100));
    }

    @Test
    void shouldSearchWithDefaultWhenNoParamsProvided() throws Exception {
        mockMvc.perform(get("/semantic-assets").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(searchService).search("", Set.of(), Set.of(), OffsetBasedPageRequest.of(0, 10));
    }

    @Test
    void shouldReturn400WhenOffsetIsLessThan0() throws Exception {
        mockMvc.perform(get("/semantic-assets")
                        .param("q", "searchText")
                        .param("offset", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));

        verifyNoInteractions(searchService);
    }

    @Test
    void shouldReturn400WhenLimitIsLessThan1() throws Exception {
        mockMvc.perform(get("/semantic-assets")
                        .param("q", "searchText")
                        .param("limit", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));

        verifyNoInteractions(searchService);
    }

    @Test
    void shouldReturn400WhenLimitIsMoreThan200() throws Exception {
        mockMvc.perform(get("/semantic-assets")
                        .param("q", "searchText")
                        .param("limit", "201")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));

        verifyNoInteractions(searchService);
    }

    @Test
    void shouldReturn400WhenThemeIsNotSupported() throws Exception {
        mockMvc.perform(get("/semantic-assets")
                        .param("q", "searchText")
                        .param("theme", "Science Fiction")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));

        verifyNoInteractions(searchService);
    }

    @Test
    void shouldReturn400WhenTypeIsNotSupported() throws Exception {
        mockMvc.perform(get("/semantic-assets")
                        .param("q", "searchText")
                        .param("type", "PEANUTS")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON.toString()));

        verifyNoInteractions(searchService);
    }
}
