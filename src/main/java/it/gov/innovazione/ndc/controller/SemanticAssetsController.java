package it.gov.innovazione.ndc.controller;

import com.github.jsonldjava.shaded.com.google.common.base.CaseFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.gen.dto.Direction.ASC;

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
    @Operation(tags = {"semantic-assets"},
            summary = "Retrieves the rights holders",
            description = "Retrieves the rights holders of the semantic assets.",
            operationId = "getRightsHolders",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = RightsHolder.class))
            })})
    @GetMapping(value = "/semantic-assets/rights-holders", produces = {"application/json"})
    List<RightsHolder> getRightsHolders() {
        return repositoryService.getRightsHolders();

    }

    @Operation(tags = {"semantic-assets"},
            summary = "Retrieves the statistics",
            description = "Retrieves the statistics of the semantic assets.",
            operationId = "getStats",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = SemanticAssetStats.class))
            })})
    @GetMapping(value = "/semantic-assets/stats", produces = {"application/json"})
    SemanticAssetStats getStats() {
        return SemanticAssetStats.builder()
                .totalStats(SemanticAssetStats.SemanticAssetTypeStats.builder()
                        .current(118)
                        .lastYear(115)
                        .build())
                .controlledVocabularyStats(SemanticAssetStats.SemanticAssetTypeStats.builder()
                        .current(120)
                        .lastYear(118)
                        .build())
                .ontologyStats(SemanticAssetStats.SemanticAssetTypeStats.builder()
                        .current(100)
                        .lastYear(98)
                        .build())
                .schemaStats(SemanticAssetStats.SemanticAssetTypeStats.builder()
                        .current(80)
                        .lastYear(80)
                        .build())
                .build();
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

        String property = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, sortBy.getValue());

        Pageable pageable = OffsetBasedPageRequest.of(offset, limit,
                Sort.by(direction == ASC
                        ? Sort.Order.asc(property)
                        : Sort.Order.desc(property)));

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
