package it.teamdigitale.ndc.controller;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
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
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import java.time.LocalDate;
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
            .title("some-title")
            .description("some-description")
            .modified(LocalDate.parse("2020-01-01"))
            .type(CONTROLLED_VOCABULARY)
            .rightsHolder(
                buildNodeSummary("https://example.com/rightsHolder", "example rights holder"))
            .accrualPeriodicity("some-accrual-periodicity")
            .conformsTo(List.of(buildNodeSummary("http://skos1", "skos1 name"),
                buildNodeSummary("http://skos2", "skos2 name")))
            .contactPoint(buildNodeSummary("https://example.com/contact", "mailto:test@test.com"))
            .creator(List.of(buildNodeSummary("http://creator1", "creator 1 name"),
                buildNodeSummary("http://creator2", "creator 2 name")))
            .distribution(List.of("some-distribution", "some-other-distribution"))
            .issued(LocalDate.parse("2020-01-02"))
            .endpointUrl("some-endpoint-url")
            .keyConcept("some-key-concept")
            .language(List.of("some-language", "some-other-language"))
            .publisher(List.of(buildNodeSummary("http://publisher1", "publisher 1 name"),
                buildNodeSummary("http://publisher2", "publisher 2 name")))
            .subject(List.of("some-subject", "some-other-subject"))
            .temporal("some-temporal")
            .versionInfo("some-version-info")
            .build());

        mockMvc.perform(get("/semantic-assets/details").param("iri", "some-iri"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.iri").value("some-iri"))
            .andExpect(jsonPath("$.title").value("some-title"))
            .andExpect(jsonPath("$.description").value("some-description"))
            .andExpect(jsonPath("$.modified").value("2020-01-01"))
            .andExpect(jsonPath("$.type").value("CONTROLLED_VOCABULARY"))
            .andExpect(jsonPath("$.accrualPeriodicity").value("some-accrual-periodicity"))
            .andExpect(jsonPath("$.conformsTo").isArray())
            .andExpect(jsonPath("$.conformsTo[0].iri").value("http://skos1"))
            .andExpect(jsonPath("$.conformsTo[0].summary").value("skos1 name"))
            .andExpect(jsonPath("$.conformsTo[1].iri").value("http://skos2"))
            .andExpect(jsonPath("$.conformsTo[1].summary").value("skos2 name"))
            .andExpect(jsonPath("$.contactPoint.iri").value("https://example.com/contact"))
            .andExpect(jsonPath("$.contactPoint.summary").value("mailto:test@test.com"))
            .andExpect(jsonPath("$.creator").isArray())
            .andExpect(jsonPath("$.creator[0].iri").value("http://creator1"))
            .andExpect(jsonPath("$.creator[0].summary").value("creator 1 name"))
            .andExpect(jsonPath("$.creator[1].iri").value("http://creator2"))
            .andExpect(jsonPath("$.creator[1].summary").value("creator 2 name"))
            .andExpect(jsonPath("$.distribution").isArray())
            .andExpect(jsonPath("$.distribution[0]").value("some-distribution"))
            .andExpect(jsonPath("$.distribution[1]").value("some-other-distribution"))
            .andExpect(jsonPath("$.issued").value("2020-01-02"))
            .andExpect(jsonPath("$.endpointUrl").value("some-endpoint-url"))
            .andExpect(jsonPath("$.keyConcept").value("some-key-concept"))
            .andExpect(jsonPath("$.language").isArray())
            .andExpect(jsonPath("$.language[0]").value("some-language"))
            .andExpect(jsonPath("$.language[1]").value("some-other-language"))
            .andExpect(jsonPath("$.publisher").isArray())
            .andExpect(jsonPath("$.publisher[0].iri").value("http://publisher1"))
            .andExpect(jsonPath("$.publisher[0].summary").value("publisher 1 name"))
            .andExpect(jsonPath("$.publisher[1].iri").value("http://publisher2"))
            .andExpect(jsonPath("$.publisher[1].summary").value("publisher 2 name"))
            .andExpect(jsonPath("$.subject").isArray())
            .andExpect(jsonPath("$.subject[0]").value("some-subject"))
            .andExpect(jsonPath("$.subject[1]").value("some-other-subject"))
            .andExpect(jsonPath("$.temporal").value("some-temporal"))
            .andExpect(jsonPath("$.versionInfo").value("some-version-info"));
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
                    .iri("some-iri")
                    .description("some-description")
                    .modified(LocalDate.parse("2020-01-01"))
                    .rightsHolder(buildNodeSummary("https://example.com/rightsHolder",
                        "example rights holder"))
                    .theme(List.of("study", "sports"))
                    .title("searchText contains")
                    .type(CONTROLLED_VOCABULARY)
                    .build()))
                .build());

        mockMvc.perform(get("/semantic-assets/search").param("term", "searchText"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.pageNumber").value(1))
            .andExpect(jsonPath("$.totalPages").value(5))
            .andExpect(jsonPath("$.data[0].iri").value("some-iri"))
            .andExpect(jsonPath("$.data[0].description").value("some-description"))
            .andExpect(jsonPath("$.data[0].modified").value("2020-01-01"))
            .andExpect(
                jsonPath("$.data[0].rightsHolder.iri").value("https://example.com/rightsHolder"))
            .andExpect(jsonPath("$.data[0].rightsHolder.summary").value("example rights holder"))
            .andExpect(jsonPath("$.data[0].theme[0]").value("study"))
            .andExpect(jsonPath("$.data[0].theme[1]").value("sports"))
            .andExpect(jsonPath("$.data[0].title").value("searchText contains"))
            .andExpect(jsonPath("$.data[0].type").value("CONTROLLED_VOCABULARY"));
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

    private NodeSummary buildNodeSummary(String iri, String summary) {
        return NodeSummary.builder()
            .iri(iri)
            .summary(summary)
            .build();
    }

}
