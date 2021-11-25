package it.teamdigitale.ndc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class SemanticAssetsControllerTest {

    @Mock
    private SemanticAssetSearchService service;

    @Test
    void shouldFetchResultsFromRepositoryForGivenKeyword() {
        SemanticAssetsController controller = new SemanticAssetsController(service);
        SemanticAssetSearchResult expectedResult = SemanticAssetSearchResult.builder().build();
        when(service.search(any(), any())).thenReturn(expectedResult);

        SemanticAssetSearchResult actualResult = controller.search("searchTerm", 2, 10);

        verify(service).search("searchTerm", Pageable.ofSize(10).withPage(1));
        assertThat(actualResult).isEqualTo(expectedResult);
    }
}
