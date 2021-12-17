package it.teamdigitale.ndc.model;

import it.teamdigitale.ndc.gen.dto.SearchResult;
import it.teamdigitale.ndc.gen.dto.SearchResultItem;
import it.teamdigitale.ndc.gen.dto.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchPage;

import java.util.stream.Collectors;

@Mapper
public interface SemanticAssetsMetadataMapper {
    @Mapping(source = "iri", target = "assetIri")
    SemanticAssetDetailsDto detailsToDto(SemanticAssetMetadata source);

    @Mapping(source = "iri", target = "assetIri")
    SearchResultItem resultItemToDto(SemanticAssetMetadata source);

    default SearchResult searchResultToDto(SearchPage<SemanticAssetMetadata> source) {
        Pageable resultPage = source.getPageable();
        return Builders.searchResult()
                .totalCount((int) source.getTotalElements())
                .limit(resultPage.getPageSize())
                .offset((int) resultPage.getOffset())
                .data(source.getContent()
                        .stream()
                        .map(s -> resultItemToDto(s.getContent()))
                        .collect(Collectors.toList()))
                .build();
    }
}
