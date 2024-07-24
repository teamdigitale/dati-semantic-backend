package it.gov.innovazione.ndc.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static java.util.Objects.nonNull;
import static org.springframework.data.elasticsearch.client.elc.Queries.termQuery;
import static org.springframework.data.elasticsearch.core.SearchHitSupport.searchPageFor;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SemanticAssetMetadataRepository {
    private final ElasticsearchOperations esOps;

    public SearchPage<SemanticAssetMetadata> search(String queryPattern, Set<String> types,
                                                    Set<String> themes, Set<String> rightsHolder,
                                                    Pageable pageable) {
        List<Query> queries = new ArrayList<>();

        if (StringUtils.isNotEmpty(queryPattern)) {
            queries.add(MatchQuery.of(mq -> mq.field("searchableText").query(queryPattern))._toQuery());
        }

        getTermsQuery(types, themes, rightsHolder).stream()
                .map(QueryVariant::_toQuery)
                .forEach(queries::add);

        queries.forEach(q -> log.info("Query: {}", q));

        NativeQuery query = NativeQuery.builder()
                .withQuery(BoolQuery.of(bq -> bq.must(queries))._toQuery())
                .withPageable(pageable)
                .build();

        return searchPageFor(esOps.search(query, SemanticAssetMetadata.class), pageable);
    }

    private List<TermsQuery> getTermsQuery(Set<String> types, Set<String> themes, Set<String> rightsHolder) {
        return Map.of("type", types, "themes", themes, "agencyId", rightsHolder).entrySet().stream()
                .filter(e -> nonNull(e.getValue()))
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> getTermsQuery(e.getKey(), e.getValue()))
                .toList();
    }

    private TermsQuery getTermsQuery(String field, Set<String> values) {
        return TermsQuery.of(t -> t.field(field).terms(
                terms -> terms.value(values.stream()
                        .filter(Objects::nonNull)
                        .map(FieldValue::of)
                        .toList())));
    }

    public Optional<SemanticAssetMetadata> findByIri(String iri) {
        return Optional.ofNullable(esOps.get(iri, SemanticAssetMetadata.class));
    }

    public long deleteByRepoUrl(String repoUrl) {
        return esOps.delete(
                DeleteQuery.builder(
                                NativeQuery.builder().withQuery(
                                                MatchQuery.of(mq -> mq.field("repoUrl").query(repoUrl))._toQuery())
                                        .build())
                        .build(),
                SemanticAssetMetadata.class).getDeleted();
    }

    public void save(SemanticAssetMetadata metadata) {
        esOps.save(metadata);
    }

    public List<SemanticAssetMetadata> findVocabulariesForRepoUrl(String repoUrl) {
        BoolQuery boolQuery = BoolQuery.of(
                bq -> bq.must(
                        List.of(
                                termQuery("repoUrl", repoUrl)._toQuery(),
                                termQuery("type", CONTROLLED_VOCABULARY.name())._toQuery())));

        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery._toQuery()).build();

        SearchHits<SemanticAssetMetadata> hits = esOps.search(query, SemanticAssetMetadata.class);
        return hits.get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
