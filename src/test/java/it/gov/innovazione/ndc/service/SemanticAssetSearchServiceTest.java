package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.exception.SemanticAssetNotFoundException;
import it.gov.innovazione.ndc.gen.dto.SearchResult;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.gen.dto.VocabulariesResult;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.SemanticAssetsMetadataMapperImpl;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SemanticAssetSearchServiceTest {

    @Mock
    private SemanticAssetMetadataRepository metadataRepository;

    @Mock
    SearchPage<SemanticAssetMetadata> searchPageMock;

    @Mock
    SearchHit<SemanticAssetMetadata> searchHitMock;

    private SemanticAssetSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SemanticAssetSearchService(metadataRepository, new SemanticAssetsMetadataMapperImpl());
    }

    @Test
    void shouldGetSearchResultForTerm() {
        SemanticAssetMetadata expectedData1 = SemanticAssetMetadata.builder().iri("1").build();
        SemanticAssetMetadata expectedData2 = SemanticAssetMetadata.builder().iri("2").build();

        Pageable pageable = Pageable.ofSize(10).withPage(0);
        when(metadataRepository.search(any(), any(), any(), Collections.emptySet(), any())).thenReturn(searchPageMock);
        when(searchPageMock.getPageable()).thenReturn(PageRequest.of(1, 10));
        when(searchPageMock.getTotalElements()).thenReturn(11L);
        when(searchPageMock.getContent()).thenReturn(List.of(searchHitMock, searchHitMock));
        when(searchHitMock.getContent()).thenReturn(expectedData1).thenReturn(expectedData2);

        SearchResult result =
                searchService.search("term", Set.of("ONTOLOGY", "SCHEMA"),
                        Set.of("EDUC", "AGRI"), Set.of(), pageable);

        assertThat(result.getTotalCount()).isEqualTo(11L);
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getOffset()).isEqualTo(10L);
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().stream().filter(e -> e.getAssetIri().equals("1"))).isNotNull();
        assertThat(result.getData().stream().filter(e -> e.getAssetIri().equals("2"))).isNotNull();
        verify(metadataRepository).search("term", Set.of("ONTOLOGY", "SCHEMA"),
                Set.of("EDUC", "AGRI"), Collections.emptySet(), pageable);
    }

    @Test
    void shouldFindAssetByIri() {
        when(metadataRepository.findByIri("iri"))
                .thenReturn(Optional.of(SemanticAssetMetadata.builder().iri("iri").build()));

        SemanticAssetDetails actual = searchService.findByIri("iri");

        verify(metadataRepository).findByIri("iri");
        assertThat(actual.getAssetIri()).isEqualTo("iri");
    }

    @Test
    void shouldFailWhenNotFoundByIri() {
        when(metadataRepository.findByIri("iri")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchService.findByIri("iri"))
                .isInstanceOf(SemanticAssetNotFoundException.class)
                .hasMessage("Semantic Asset not found for Iri : iri");
    }

    @Test
    void shouldGetVocabularies() {
        SemanticAssetMetadata expectedData1 = SemanticAssetMetadata.builder()
                .iri("1")
                .type(SemanticAssetType.CONTROLLED_VOCABULARY)
                .title("Some vocab")
                .build();
        SemanticAssetMetadata expectedData2 = SemanticAssetMetadata.builder()
                .iri("2")
                .type(SemanticAssetType.CONTROLLED_VOCABULARY)
                .title("Some other vocab")
                .build();

        Pageable pageable = Pageable.ofSize(10).withPage(0);
        when(metadataRepository.search(any(), any(), any(), Collections.emptySet(), any())).thenReturn(searchPageMock);
        when(searchPageMock.getPageable()).thenReturn(PageRequest.of(1, 10));
        when(searchPageMock.getTotalElements()).thenReturn(11L);
        when(searchPageMock.getContent()).thenReturn(List.of(searchHitMock, searchHitMock));
        when(searchHitMock.getContent()).thenReturn(expectedData1).thenReturn(expectedData2);

        VocabulariesResult result = searchService.getVocabularies(pageable);

        assertThat(result.getTotalCount()).isEqualTo(11L);
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getOffset()).isEqualTo(10L);
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().stream().filter(e -> e.getTitle().equals("Some vocab"))).isNotNull();
        assertThat(result.getData().stream().filter(e -> e.getTitle().equals("Some other vocab"))).isNotNull();
        verify(metadataRepository).search("", Set.of("CONTROLLED_VOCABULARY"), emptySet(), Collections.emptySet(), pageable);
    }
}
