package it.gov.innovazione.ndc.model;

import it.gov.innovazione.ndc.gen.dto.SearchResult;
import it.gov.innovazione.ndc.gen.dto.SearchResultItem;
import it.gov.innovazione.ndc.gen.dto.SemanticAssetDetails;
import it.gov.innovazione.ndc.gen.dto.Theme;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import lombok.SneakyThrows;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchPage;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface SemanticAssetsMetadataMapper {
    @Mapping(source = "iri", target = "assetIri")
    SemanticAssetDetails detailsToDto(SemanticAssetMetadata source);

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

    default Set<Theme> stringsToThemeSet(List<String> themeString) {
        if (themeString == null) {
            return null;
        }
        return themeString.stream().map(this::maybeTheme)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @SneakyThrows
    default URI stringToURI(String s) {
        return Objects.isNull(s) ? null : new URI(s);
    }

    private Theme maybeTheme(String str) {
        try {
            return Theme.fromValue(str);
        } catch (Exception e) {
            return null;
        }
    }
}
