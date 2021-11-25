package it.teamdigitale.ndc.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.teamdigitale.ndc.controller.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.controller.dto.SemanticAssetsSearchDto;
import it.teamdigitale.ndc.controller.exception.SemanticAssetNotFoundException;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SemanticAssetsController.class)
public class SemanticAssetsControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SemanticAssetSearchService searchService;

    @Test
    void shouldFindByIri() throws Exception {
        when(searchService.findByIri("some-iri")).thenReturn(SemanticAssetDetailsDto.builder()
            .iri("some-iri")
            .build());

        mockMvc.perform(get("/semantic-assets/details").param("iri", "some-iri"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(content().json("{\"iri\":\"some-iri\"}"));
    }

    @Test
    void shouldReturn404WhenSemanticAssetNotFound() throws Exception {
        when(searchService.findByIri("some-iri")).thenThrow(
            new SemanticAssetNotFoundException("some-iri"));

        mockMvc.perform(get("/semantic-assets/details").param("iri", "some-iri"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("Semantic Asset not found for Iri : some-iri"));
    }

    @Test
    void shouldReturnMatchingAssetsUsingDefaultPageParams() throws Exception {
        when(searchService.search("searchText", Pageable.ofSize(10).withPage(0))).thenReturn(
            SemanticAssetSearchResult.builder()
                .pageNumber(1)
                .totalPages(5)
                .data(List.of(SemanticAssetsSearchDto.builder()
                    .title("searchText contains")
                    .build()))
                .build());

        mockMvc.perform(get("/semantic-assets/search").param("term", "searchText"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.pageNumber").value(1))
            .andExpect(jsonPath("$.totalPages").value(5))
            .andExpect(jsonPath("$.data[0].title").value("searchText contains"));
    }

    @Test
    void shouldReturn400WhenPageNumberIsLessThan1() throws Exception {
        mockMvc.perform(get("/semantic-assets/search")
                .param("term", "searchText")
                .param("page_number", "0"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/json"));

        verifyNoInteractions(searchService);
    }

    @Test
    void shouldReturn400WhenPageSizeIsLessThan1() throws Exception {
        mockMvc.perform(get("/semantic-assets/search")
                .param("term", "searchText")
                .param("page_size", "0"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/json"));

        verifyNoInteractions(searchService);
    }

    @Test
    void shouldReturn400WhenPageSizeIsMoreThan200() throws Exception {
        mockMvc.perform(get("/semantic-assets/search")
                .param("term", "searchText")
                .param("page_size", "201"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/json"));

        verifyNoInteractions(searchService);
    }

}
