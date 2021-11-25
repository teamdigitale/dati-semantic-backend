package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/semantic-assets")
@RequiredArgsConstructor
public class SemanticAssetsController {

    private final SemanticAssetSearchService searchService;

    @GetMapping("/search")
    public SemanticAssetSearchResult search(
        @RequestParam("term") String term,
        @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
        @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        int pageIndex = pageNumber - 1;
        return searchService.search(term, Pageable.ofSize(pageSize).withPage(pageIndex));
    }
}
