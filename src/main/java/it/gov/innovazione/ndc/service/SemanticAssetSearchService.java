package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.model.SemanticAssetsMetadataMapper;
import it.gov.innovazione.ndc.controller.exception.SemanticAssetNotFoundException;
import it.gov.innovazione.ndc.gen.dto.SearchResult;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
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

    public SearchResult search(String queryPattern, Set<String> types,
                               Set<String> themes, Pageable pageable) {

        SearchPage<SemanticAssetMetadata> searchResults =
                metadataRepository.search(queryPattern, types, themes, pageable);

        return mapper.searchResultToDto(searchResults);
    }

    public SemanticAssetDetails findByIri(String iri) {
        return metadataRepository.findByIri(iri)
                .map(mapper::detailsToDto)
                .orElseThrow(() -> new SemanticAssetNotFoundException(iri));
    }
}
