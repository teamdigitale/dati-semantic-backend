package it.teamdigitale.ndc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.gen.model.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.gen.model.SemanticAssetSearchResult;
import it.teamdigitale.ndc.model.ModelBuilder;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;

@ExtendWith(MockitoExtension.class)
public class SemanticAssetsControllerTest {

    @Mock
    private SemanticAssetSearchService service;

    @Test
    void shouldFetchResultsFromRepositoryForGivenKeyword() {
        SemanticAssetsController controller = new SemanticAssetsController(service);
        SemanticAssetSearchResult expectedResult = ModelBuilder.searchResultBuilder().build();
        when(service.search(any(), any(), any(), any())).thenReturn(expectedResult);

        SemanticAssetSearchResult actualResult =
            controller.search("searchTerm", 0, 10,
                Set.of("CONTROLLED_VOCABULARY"),
                Set.of("EDUC")).getBody();

        verify(service).search("searchTerm",
            Set.of("CONTROLLED_VOCABULARY"),
            Set.of("EDUC"),
            OffsetBasedPageRequest.of(0, 10));
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    void shouldGetDetailsOfTheAssetByIri() {
        SemanticAssetsController controller = new SemanticAssetsController(service);
        SemanticAssetDetailsDto expected = new SemanticAssetDetailsDto();
        when(service.findByIri(any())).thenReturn(expected);

        SemanticAssetDetailsDto actualResult = controller.getDetails("iri").getBody();

        verify(service).findByIri("iri");
        assertThat(actualResult).isEqualTo(expected);
    }
}
