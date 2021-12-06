package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.controller.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import java.util.Set;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/semantic-assets")
@RequiredArgsConstructor
@Validated
public class SemanticAssetsController {
    private final SemanticAssetSearchService searchService;

    @GetMapping
    public SemanticAssetSearchResult search(
        @RequestParam(value = "q", defaultValue = "") String queryPattern,
        @RequestParam(value = "offset", defaultValue = "0")
        @Min(0) @Max(30000) Integer offset,
        @RequestParam(value = "limit", defaultValue = "10")
        @Min(1) @Max(200) Integer limit,
        @RequestParam(value = "type", defaultValue = "") Set<String> types,
        @RequestParam(value = "theme", defaultValue = "") Set<String> themes) {

        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);

        return searchService.search(queryPattern, types, themes, pageable);
    }

    @GetMapping("/byIri")
    public SemanticAssetDetailsDto getDetails(@RequestParam("iri") String iri) {
        return searchService.findByIri(iri);
    }
}
