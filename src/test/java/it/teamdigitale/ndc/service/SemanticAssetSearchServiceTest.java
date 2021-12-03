package it.teamdigitale.ndc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.controller.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.controller.exception.SemanticAssetNotFoundException;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;


@ExtendWith(MockitoExtension.class)
class SemanticAssetSearchServiceTest {

    @Mock
    private SemanticAssetMetadataRepository metadataRepository;

    @Mock
    SearchPage<SemanticAssetMetadata> searchPageMock;

    @Mock
    SearchHit<SemanticAssetMetadata> searchHitMock;

    @InjectMocks
    private SemanticAssetSearchService searchService;

    @Test
    void shouldGetSearchResultForTerm() {
        SemanticAssetMetadata expectedData1 = SemanticAssetMetadata.builder()
            .iri("1").build();
        SemanticAssetMetadata expectedData2 = SemanticAssetMetadata.builder()
            .iri("2").build();

        Pageable pageable = Pageable.ofSize(10).withPage(0);
        when(metadataRepository.search(any(), any(), any(), any())).thenReturn(searchPageMock);
        when(searchPageMock.getPageable()).thenReturn(PageRequest.of(0, 10));
        when(searchPageMock.getTotalPages()).thenReturn(1);
        when(searchPageMock.getContent()).thenReturn(List.of(searchHitMock, searchHitMock));
        when(searchHitMock.getContent()).thenReturn(expectedData1).thenReturn(expectedData2);

        SemanticAssetSearchResult result =
            searchService.search("term", Set.of("ONTOLOGY", "SCHEMA"),
                Set.of("EDUC", "AGRI"), pageable);

        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().stream().filter(e -> e.getAssetIri().equals("1"))).isNotNull();
        assertThat(result.getData().stream().filter(e -> e.getAssetIri().equals("2"))).isNotNull();
        verify(metadataRepository).search("term", Set.of("ONTOLOGY", "SCHEMA"),
            Set.of("EDUC", "AGRI"), pageable);
    }

    @Test
    void shouldFindAssetByIri() {
        when(metadataRepository.findByIri("iri"))
            .thenReturn(Optional.of(SemanticAssetMetadata.builder().iri("iri").build()));

        SemanticAssetDetailsDto actual = searchService.findByIri("iri");

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
}