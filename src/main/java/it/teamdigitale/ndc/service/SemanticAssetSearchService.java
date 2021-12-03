package it.teamdigitale.ndc.service;

import it.teamdigitale.ndc.controller.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.controller.dto.SemanticAssetsSearchDto;
import it.teamdigitale.ndc.controller.exception.SemanticAssetNotFoundException;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticAssetSearchService {
    private final SemanticAssetMetadataRepository metadataRepository;

    public SemanticAssetSearchResult search(String term, Set<String> types,
                                            Set<String> themes, Pageable pageable) {

        SearchPage<SemanticAssetMetadata> searchResults =
            metadataRepository.search(term, types, themes, pageable);

        return SemanticAssetSearchResult.builder()
            .pageNumber(searchResults.getPageable().getPageNumber() + 1)
            .totalPages(searchResults.getTotalPages())
            .data(searchResults.getContent()
                .stream()
                .map(s -> SemanticAssetsSearchDto.from(s.getContent()))
                .collect(Collectors.toList()))
            .build();
    }

    public SemanticAssetDetailsDto findByIri(String iri) {
        return metadataRepository.findByIri(iri)
            .map(SemanticAssetDetailsDto::from)
            .orElseThrow(() -> new SemanticAssetNotFoundException(iri));
    }
}
