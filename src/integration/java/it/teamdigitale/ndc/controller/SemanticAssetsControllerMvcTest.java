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
            .assetIri("some-iri")
            .title("some-title")
            .description("some-description")
            .themes(List.of("some-theme", "some-other-theme"))
            .modifiedOn(LocalDate.parse("2020-01-01"))
            .type(CONTROLLED_VOCABULARY)
            .rightsHolder(
                buildNodeSummary("https://example.com/rightsHolder", "example rights holder"))
            .accrualPeriodicity("some-accrual-periodicity")
            .conformsTo(List.of(buildNodeSummary("http://skos1", "skos1 name"),
                buildNodeSummary("http://skos2", "skos2 name")))
            .contactPoint(buildNodeSummary("https://example.com/contact", "mailto:test@test.com"))
            .creators(List.of(buildNodeSummary("http://creator1", "creator 1 name"),
                buildNodeSummary("http://creator2", "creator 2 name")))
            .distributionUrls(List.of("some-distribution", "some-other-distribution"))
            .issuedOn(LocalDate.parse("2020-01-02"))
            .endpointUrl("some-endpoint-url")
            .keyConcept("some-key-concept")
            .languages(List.of("some-language", "some-other-language"))
            .publishers(List.of(buildNodeSummary("http://publisher1", "publisher 1 name"),
                buildNodeSummary("http://publisher2", "publisher 2 name")))
            .subjects(List.of("some-subject", "some-other-subject"))
            .versionInfo("some-version-info")
            .keyClasses(List.of(buildNodeSummary("http://Class1", "Class1"), buildNodeSummary("http://Class2", "Class2")))
            .prefix("prefix")
            .projects(List.of(buildNodeSummary("http://project1", "project1"), buildNodeSummary("http://project2", "project2")))
            .build());

        mockMvc.perform(get("/semantic-assets/details").param("iri", "some-iri"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.assetIri").value("some-iri"))
            .andExpect(jsonPath("$.title").value("some-title"))
            .andExpect(jsonPath("$.description").value("some-description"))
            .andExpect(jsonPath("$.themes").isArray())
            .andExpect(jsonPath("$.themes[0]").value("some-theme"))
            .andExpect(jsonPath("$.themes[1]").value("some-other-theme"))
            .andExpect(jsonPath("$.modifiedOn").value("2020-01-01"))
            .andExpect(jsonPath("$.type").value("CONTROLLED_VOCABULARY"))
            .andExpect(jsonPath("$.accrualPeriodicity").value("some-accrual-periodicity"))
            .andExpect(jsonPath("$.conformsTo").isArray())
            .andExpect(jsonPath("$.conformsTo[0].iri").value("http://skos1"))
            .andExpect(jsonPath("$.conformsTo[0].summary").value("skos1 name"))
            .andExpect(jsonPath("$.conformsTo[1].iri").value("http://skos2"))
            .andExpect(jsonPath("$.conformsTo[1].summary").value("skos2 name"))
            .andExpect(jsonPath("$.contactPoint.iri").value("https://example.com/contact"))
            .andExpect(jsonPath("$.contactPoint.summary").value("mailto:test@test.com"))
            .andExpect(jsonPath("$.creators").isArray())
            .andExpect(jsonPath("$.creators[0].iri").value("http://creator1"))
            .andExpect(jsonPath("$.creators[0].summary").value("creator 1 name"))
            .andExpect(jsonPath("$.creators[1].iri").value("http://creator2"))
            .andExpect(jsonPath("$.creators[1].summary").value("creator 2 name"))
            .andExpect(jsonPath("$.distributionUrls").isArray())
            .andExpect(jsonPath("$.distributionUrls[0]").value("some-distribution"))
            .andExpect(jsonPath("$.distributionUrls[1]").value("some-other-distribution"))
            .andExpect(jsonPath("$.issuedOn").value("2020-01-02"))
            .andExpect(jsonPath("$.endpointUrl").value("some-endpoint-url"))
            .andExpect(jsonPath("$.keyConcept").value("some-key-concept"))
            .andExpect(jsonPath("$.languages").isArray())
            .andExpect(jsonPath("$.languages[0]").value("some-language"))
            .andExpect(jsonPath("$.languages[1]").value("some-other-language"))
            .andExpect(jsonPath("$.publishers").isArray())
            .andExpect(jsonPath("$.publishers[0].iri").value("http://publisher1"))
            .andExpect(jsonPath("$.publishers[0].summary").value("publisher 1 name"))
            .andExpect(jsonPath("$.publishers[1].iri").value("http://publisher2"))
            .andExpect(jsonPath("$.publishers[1].summary").value("publisher 2 name"))
            .andExpect(jsonPath("$.subjects").isArray())
            .andExpect(jsonPath("$.subjects[0]").value("some-subject"))
            .andExpect(jsonPath("$.subjects[1]").value("some-other-subject"))
            .andExpect(jsonPath("$.temporal").doesNotExist())
            .andExpect(jsonPath("$.versionInfo").value("some-version-info"))
            .andExpect(jsonPath("$.keyClasses").isArray())
            .andExpect(jsonPath("$.keyClasses[0].iri").value("http://Class1"))
            .andExpect(jsonPath("$.keyClasses[0].summary").value("Class1"))
            .andExpect(jsonPath("$.keyClasses[1].iri").value("http://Class2"))
            .andExpect(jsonPath("$.keyClasses[1].summary").value("Class2"))
            .andExpect(jsonPath("$.prefix").value("prefix"))
            .andExpect(jsonPath("$.projects").isArray())
            .andExpect(jsonPath("$.projects[0].iri").value("http://project1"))
            .andExpect(jsonPath("$.projects[0].summary").value("project1"))
            .andExpect(jsonPath("$.projects[1].iri").value("http://project2"))
            .andExpect(jsonPath("$.projects[1].summary").value("project2"));

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
                    .assetIri("some-iri")
                    .description("some-description")
                    .modified(LocalDate.parse("2020-01-01"))
                    .rightsHolder(buildNodeSummary("https://example.com/rightsHolder",
                        "example rights holder"))
                    .themes(List.of("study", "sports"))
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
            .andExpect(jsonPath("$.data[0].assetIri").value("some-iri"))
            .andExpect(jsonPath("$.data[0].description").value("some-description"))
            .andExpect(jsonPath("$.data[0].modified").value("2020-01-01"))
            .andExpect(
                jsonPath("$.data[0].rightsHolder.iri").value("https://example.com/rightsHolder"))
            .andExpect(jsonPath("$.data[0].rightsHolder.summary").value("example rights holder"))
            .andExpect(jsonPath("$.data[0].themes[0]").value("study"))
            .andExpect(jsonPath("$.data[0].themes[1]").value("sports"))
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
