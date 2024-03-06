package it.gov.innovazione.ndc.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.gov.innovazione.ndc.gen.api.SemanticAssetsApi;
import it.gov.innovazione.ndc.gen.dto.AssetType;
import it.gov.innovazione.ndc.gen.dto.Direction;
import it.gov.innovazione.ndc.gen.dto.SearchResult;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.gen.dto.SortBy;
import it.gov.innovazione.ndc.gen.dto.Theme;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.service.SemanticAssetSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class SemanticAssetsController implements SemanticAssetsApi {
    private final SemanticAssetSearchService searchService;
    private final RepositoryService repositoryService;

    /**
     * GET /semantic-assets/rights-holders
     * Retrieves the rights holders of the semantic assets.
     *
     * @return OK (status code 200)
     */
    @ApiOperation(value = "", nickname = "getRightsHolders", notes = "Retrieves the rights holders",
            response = SemanticAssetDetails.class, tags = {"semantic-assets",})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = SemanticAssetDetails.class)})
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/semantic-assets/rights-holders",
            produces = {"application/json"}
    )
    List<RightsHolder> getRightsHolders() {
        return repositoryService.getRightsHolders();

    }

    @Override
    public ResponseEntity<SearchResult> search(
            String q,
            Integer offset,
            Integer limit,
            SortBy sortBy,
            Direction direction,
            Set<AssetType> type,
            Set<Theme> theme,
            Set<String> rightsHolder) {

        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);

        return AppJsonResponse.ok(
                searchService.search(q,
                        toEnumStrings(type, AssetType::getValue),
                        toEnumStrings(theme, Theme::getValue),
                        rightsHolder,
                        pageable
                )
        );
    }

    private <T> Set<String> toEnumStrings(Set<T> parameter, Function<T, String> valueMapper) {
        Set<String> themeStrings;
        if (parameter == null) {
            themeStrings = Collections.emptySet();
        } else {
            themeStrings = parameter.stream().map(valueMapper).collect(Collectors.toSet());
        }
        return themeStrings;
    }

    @Override
    public ResponseEntity<SemanticAssetDetails> getDetails(URI iri) {
        return AppJsonResponse.ok(searchService.findByIri(iri.toString()));
    }

}
