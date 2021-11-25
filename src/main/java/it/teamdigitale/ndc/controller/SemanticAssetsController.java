package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.controller.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
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

    @GetMapping("/search")
    public SemanticAssetSearchResult search(
        @RequestParam("term") String term,
        @RequestParam(value = "page_number", defaultValue = "1")
        @Min(1) Integer pageNumber,
        @RequestParam(value = "page_size", defaultValue = "10")
        @Min(1) @Max(200) Integer pageSize) {
        int pageIndex = pageNumber - 1;
        return searchService.search(term, Pageable.ofSize(pageSize).withPage(pageIndex));
    }

    @GetMapping("/details")
    public SemanticAssetDetailsDto getDetails(@RequestParam("iri") String iri) {
        return searchService.findByIri(iri);
    }
}
