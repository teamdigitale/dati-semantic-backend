package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.gen.dto.SearchResult;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.gen.dto.Theme;
import it.gov.innovazione.ndc.model.Builders;
import it.gov.innovazione.ndc.service.SemanticAssetSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static it.gov.innovazione.ndc.gen.dto.AssetType.CONTROLLED_VOCABULARY;
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
        when(service.search(any(), any(), any(), any(), any())).thenReturn(expectedResult);

        SearchResult actualResult =
            controller.search("searchTerm", 0, 10,
                    null, null,
                Set.of(CONTROLLED_VOCABULARY),
                    Set.of(Theme.EDUC),
                    null).getBody();

        verify(service).search("searchTerm",
            Set.of("CONTROLLED_VOCABULARY"),
            Set.of("http://publications.europa.eu/resource/authority/data-theme/EDUC"),
                Set.of(),
            OffsetBasedPageRequest.of(0, 10));
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    void shouldGetDetailsOfTheAssetByIri() throws URISyntaxException {
        SemanticAssetsController controller = new SemanticAssetsController(service);
        SemanticAssetDetails expected = new SemanticAssetDetails();
        when(service.findByIri(any())).thenReturn(expected);

        SemanticAssetDetails actualResult = controller.getDetails(new URI("iri")).getBody();

        verify(service).findByIri("iri");
        assertThat(actualResult).isEqualTo(expected);
    }
}
