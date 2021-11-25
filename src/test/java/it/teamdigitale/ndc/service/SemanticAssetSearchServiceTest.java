package it.teamdigitale.ndc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;


@ExtendWith(MockitoExtension.class)
class SemanticAssetSearchServiceTest {

    @Mock
    private SemanticAssetMetadataRepository metadataRepository;

    @InjectMocks
    private SemanticAssetSearchService searchService;

    @Test
    void shouldGetSearchResult() {
        SemanticAssetMetadata expectedData1 = SemanticAssetMetadata.builder()
            .iri("1").build();
        SemanticAssetMetadata expectedData2 = SemanticAssetMetadata.builder()
            .iri("2").build();
        Pageable pageable = Pageable.ofSize(10).withPage(0);
        when(metadataRepository.findBySearchableTextContaining(any(), any()))
            .thenReturn(new PageImpl<>(List.of(expectedData1, expectedData2), pageable, 2));

        SemanticAssetSearchResult result = searchService.search("term", pageable);

        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().stream().filter(e -> e.getIri().equals("1"))).isNotNull();
        assertThat(result.getData().stream().filter(e -> e.getIri().equals("2"))).isNotNull();
        verify(metadataRepository).findBySearchableTextContaining("term", pageable);
    }
}