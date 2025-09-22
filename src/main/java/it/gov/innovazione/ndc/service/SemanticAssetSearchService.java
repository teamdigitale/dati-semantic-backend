package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.MltRequest;
import it.gov.innovazione.ndc.controller.exception.SemanticAssetNotFoundException;
import it.gov.innovazione.ndc.gen.dto.AssetType;
import it.gov.innovazione.ndc.gen.dto.SearchResult;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.gen.dto.VocabulariesResult;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.SemanticAssetsMetadataMapper;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SemanticAssetSearchService {
    private final SemanticAssetMetadataRepository metadataRepository;
    private final SemanticAssetsMetadataMapper mapper;

    public SearchResult search(String queryPattern, Set<String> types,
                               Set<String> themes, Set<String> rightsHolder, Pageable pageable) {

        SearchPage<SemanticAssetMetadata> searchResults =
                metadataRepository.search(queryPattern, types, themes, rightsHolder, pageable);

        return mapper.searchResultToDto(searchResults);
    }

    public SemanticAssetDetails findByIri(String iri) {
        return metadataRepository.findByIri(iri)
                .map(mapper::detailsToDto)
                .orElseThrow(() -> new SemanticAssetNotFoundException(iri));
    }

    public VocabulariesResult getVocabularies(Pageable pageable) {
        SearchPage<SemanticAssetMetadata> results = metadataRepository.search("",
                Set.of(AssetType.CONTROLLED_VOCABULARY.getValue()),
                Collections.emptySet(), Collections.emptySet(), pageable);
        return mapper.vocabResultToDto(results);
    }

    public SearchResult moreLikeThis(MltRequest req) {

        // in SemanticAssetSearchService.moreLikeThis
        SemanticAssetMetadata semanticAssetMetadata = metadataRepository.findByIri(req.assetIri())
                .orElseThrow(() -> new SemanticAssetNotFoundException(req.assetIri()));

        Pageable pageable = it.gov.innovazione.ndc.controller.OffsetBasedPageRequest.of(
                req.offset(), req.limit(), Sort.unsorted());

        var page = metadataRepository.moreLikeThis(
                semanticAssetMetadata.getElasticsearchId(),
                req.assetIri(),
                req.minTermFreq(),
                req.minDocFreq(),
                req.maxQueryTerms(),
                req.minimumShouldMatch(),
                pageable
        );

        return mapper.searchResultToDto(page);
    }
}
