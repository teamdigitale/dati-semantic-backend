package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.gen.api.SemanticAssetsApi;
import it.teamdigitale.ndc.gen.dto.AssetType;
import it.teamdigitale.ndc.gen.dto.SearchResult;
import it.teamdigitale.ndc.gen.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.gen.dto.Theme;
import it.teamdigitale.ndc.service.SemanticAssetSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class SemanticAssetsController implements SemanticAssetsApi {
    private final SemanticAssetSearchService searchService;

    @Override
    public ResponseEntity<SearchResult> search(String q, Integer offset, Integer limit, Set<AssetType> type, Set<Theme> theme) {
        Pageable pageable = OffsetBasedPageRequest.of(offset, limit);

        return ResponseEntity.ok(
                searchService.search(q,
                        toEnumStrings(type, AssetType::getValue),
                        toEnumStrings(theme, Theme::getValue),
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
    public ResponseEntity<SemanticAssetDetailsDto> getDetails(URI iri) {
        return ResponseEntity.ok(searchService.findByIri(iri.toString()));
    }

    private Set<String> nullToEmpty(Set<String> s) {
        return Objects.requireNonNullElseGet(s, Set::of);
    }
}
