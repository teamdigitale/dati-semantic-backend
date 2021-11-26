package it.teamdigitale.ndc.service;

import it.teamdigitale.ndc.controller.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.controller.dto.SemanticAssetsSearchDto;
import it.teamdigitale.ndc.controller.exception.SemanticAssetNotFoundException;
import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticAssetSearchService {
    private final SemanticAssetMetadataRepository metadataRepository;

    public SemanticAssetSearchResult search(String term, Pageable pageable) {
        Page<SemanticAssetMetadata> page =
            metadataRepository.findBySearchableText(term, pageable);
        return SemanticAssetSearchResult.builder()
            .pageNumber(page.getNumber() + 1)
            .totalPages(page.getTotalPages())
            .data(page.stream()
                .map(SemanticAssetsSearchDto::from)
                .collect(Collectors.toList()))
            .build();
    }

    public SemanticAssetDetailsDto findByIri(String iri) {
        return metadataRepository.findByIri(iri)
            .map(SemanticAssetDetailsDto::from)
            .orElseThrow(() -> new SemanticAssetNotFoundException(iri));
    }
}
