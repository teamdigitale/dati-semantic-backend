package it.gov.innovazione.ndc.repository;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.springframework.data.elasticsearch.core.SearchHitSupport.searchPageFor;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SemanticAssetMetadataRepository {
    private final ElasticsearchOperations esOps;

    public SearchPage<SemanticAssetMetadata> search(String queryPattern, Set<String> types,
                                                    Set<String> themes, Pageable pageable) {
        BoolQueryBuilder boolQuery =
                new BoolQueryBuilder().must(matchQuery("searchableText", queryPattern));

        addFilters(types, themes, boolQuery);

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(pageable)
                .build();
        return searchPageFor(esOps.search(query, SemanticAssetMetadata.class), pageable);
    }

    public Optional<SemanticAssetMetadata> findByIri(String iri) {
        return Optional.ofNullable(esOps.get(iri, SemanticAssetMetadata.class));
    }

    public long deleteByRepoUrl(String repoUrl) {
        return esOps.delete(new NativeSearchQuery(matchQuery("repoUrl", repoUrl)),
                SemanticAssetMetadata.class).getDeleted();
    }

    public void save(SemanticAssetMetadata metadata) {
        esOps.save(metadata);
    }

    private void addFilters(Set<String> types, Set<String> themes, BoolQueryBuilder finalQuery) {
        if (!types.isEmpty()) {
            finalQuery.filter(new TermsQueryBuilder("type", types));
        }
        if (!themes.isEmpty()) {
            finalQuery.filter(new TermsQueryBuilder("themes", themes));
        }
    }

    private QueryBuilder matchQuery(String field, String value) {
        QueryBuilder textSearch;
        if (ObjectUtils.isEmpty(value)) {
            textSearch = new MatchAllQueryBuilder();
        } else {
            textSearch = new MatchQueryBuilder(field, value)
                    .fuzziness(Fuzziness.AUTO);
        }
        return textSearch;
    }

    public List<SemanticAssetMetadata> findVocabulariesForRepoUrl(String repoUrl) {
        QueryBuilder queryBuilder = boolQuery()
                .must(termQuery("repoUrl", repoUrl))
                .must(termQuery("type", CONTROLLED_VOCABULARY.name()));
        NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
        SearchHits<SemanticAssetMetadata> hits = esOps.search(query, SemanticAssetMetadata.class);
        return hits.get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
