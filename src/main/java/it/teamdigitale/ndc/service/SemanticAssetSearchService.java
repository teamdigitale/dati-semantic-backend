package it.teamdigitale.ndc.service;

import it.teamdigitale.ndc.controller.exception.SemanticAssetNotFoundException;
import it.teamdigitale.ndc.gen.model.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.gen.model.SemanticAssetSearchResult;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.model.SemanticAssetsMetadataMapper;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SemanticAssetSearchService {
    private final SemanticAssetMetadataRepository metadataRepository;
    private final SemanticAssetsMetadataMapper mapper;

    public SemanticAssetSearchResult search(String queryPattern, Set<String> types,
                                            Set<String> themes, Pageable pageable) {

        SearchPage<SemanticAssetMetadata> searchResults =
                metadataRepository.search(queryPattern, types, themes, pageable);

        return mapper.searchResultToDto(searchResults);
    }

    public SemanticAssetDetailsDto findByIri(String iri) {
        return metadataRepository.findByIri(iri)
                .map(mapper::detailsToDto)
                .orElseThrow(() -> new SemanticAssetNotFoundException(iri));
    }
}
