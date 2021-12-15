package it.teamdigitale.ndc.model;

import it.teamdigitale.ndc.gen.model.SemanticAssetDetailsDto;
import it.teamdigitale.ndc.gen.model.SemanticAssetSearchResult;
import it.teamdigitale.ndc.gen.model.SemanticAssetsSearchDto;
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
    SemanticAssetsSearchDto searchToDto(SemanticAssetMetadata source);

    default SemanticAssetSearchResult searchResultToDto(SearchPage<SemanticAssetMetadata> source) {
        Pageable resultPage = source.getPageable();
        return ModelBuilder.searchResultBuilder()
                .totalCount(source.getTotalElements())
                .limit(resultPage.getPageSize())
                .offset(resultPage.getOffset())
                .data(source.getContent()
                        .stream()
                        .map(s -> searchToDto(s.getContent()))
                        .collect(Collectors.toList()))
                .build();
    }
}
