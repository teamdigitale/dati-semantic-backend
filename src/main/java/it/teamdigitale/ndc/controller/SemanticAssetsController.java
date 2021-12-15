package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.gen.api.SemanticAssetsApi;
import it.teamdigitale.ndc.gen.model.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.gen.model.SemanticAssetSearchResult;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
@RestController
public class SemanticAssetsController implements SemanticAssetsApi {
    private final SemanticAssetSearchService searchService;

    @Override
    public ResponseEntity<SemanticAssetSearchResult> search(String q, Integer offset, Integer limit, Set<String> type, Set<String> theme) {
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);

        return ResponseEntity.ok(searchService.search(q, nullToEmpty(type), nullToEmpty(theme), pageable));
    }

    @Override
    public ResponseEntity<SemanticAssetDetailsDto> getDetails(String iri) {
        return ResponseEntity.ok(searchService.findByIri(iri));
    }

    private Set<String> nullToEmpty(Set<String> s) {
        return Objects.requireNonNullElseGet(s, Set::of);
    }
}
