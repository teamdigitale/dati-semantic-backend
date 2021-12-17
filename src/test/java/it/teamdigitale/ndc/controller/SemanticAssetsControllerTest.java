package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.gen.dto.SearchResult;
import it.teamdigitale.ndc.gen.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.gen.dto.Theme;
import it.teamdigitale.ndc.model.Builders;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SemanticAssetsControllerTest {

    @Mock
    private SemanticAssetSearchService service;

    @Test
    void shouldFetchResultsFromRepositoryForGivenKeyword() {
        SemanticAssetsController controller = new SemanticAssetsController(service);
        SearchResult expectedResult = Builders.searchResult().build();
        when(service.search(any(), any(), any(), any())).thenReturn(expectedResult);

        SearchResult actualResult =
            controller.search("searchTerm", 0, 10,
                Set.of("CONTROLLED_VOCABULARY"),
                Set.of(Theme.EDUC)).getBody();

        verify(service).search("searchTerm",
            Set.of("CONTROLLED_VOCABULARY"),
            Set.of("http://publications.europa.eu/resource/authority/data-theme/EDUC"),
            OffsetBasedPageRequest.of(0, 10));
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    void shouldGetDetailsOfTheAssetByIri() throws URISyntaxException {
        SemanticAssetsController controller = new SemanticAssetsController(service);
        SemanticAssetDetailsDto expected = new SemanticAssetDetailsDto();
        when(service.findByIri(any())).thenReturn(expected);

        SemanticAssetDetailsDto actualResult = controller.getDetails(new URI("iri")).getBody();

        verify(service).findByIri("iri");
        assertThat(actualResult).isEqualTo(expected);
    }
}
