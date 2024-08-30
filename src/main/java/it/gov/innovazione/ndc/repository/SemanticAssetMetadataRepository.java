package it.gov.innovazione.ndc.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.service.InstanceManager;
import it.gov.innovazione.ndc.service.InstanceManager.RepositoryInstance;
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
import java.util.stream.Stream;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static java.util.Objects.nonNull;
import static org.springframework.data.elasticsearch.client.elc.Queries.termQuery;
import static org.springframework.data.elasticsearch.core.SearchHitSupport.searchPageFor;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SemanticAssetMetadataRepository {
    private final ElasticsearchOperations esOps;
    private final InstanceManager instanceManager;

    public SearchPage<SemanticAssetMetadata> search(String queryPattern, Set<String> types,
                                                    Set<String> themes, Set<String> rightsHolder,
                                                    Pageable pageable) {
        List<Query> queries = new ArrayList<>();

        if (StringUtils.isNotEmpty(queryPattern)) {
            queries.add(MatchQuery.of(mq -> mq.field("searchableText").query(queryPattern))._toQuery());
        }

        getQueriesForParams(types, themes, rightsHolder).stream()
                .map(QueryVariant::_toQuery)
                .forEach(queries::add);

        getConditionForInstances()
                .map(QueryVariant::_toQuery)
                .ifPresent(queries::add);

        NativeQuery query = NativeQuery.builder()
                .withQuery(BoolQuery.of(bq -> bq.must(queries))._toQuery())
                .withPageable(pageable)
                .build();

        log.info("Searching for assets with query: {}", query);

        return searchPageFor(esOps.search(query, SemanticAssetMetadata.class), pageable);
    }

    private Optional<BoolQuery> getConditionForInstances() {
        List<RepositoryInstance> currentRepoInstances = instanceManager.getCurrentInstances();

        List<Query> repoInstanceQueries = currentRepoInstances.stream()
                .map(repositoryInstance -> getBoolQueryForRepo(
                        repositoryInstance.getUrl(),
                        repositoryInstance.getInstance()))
                .map(QueryVariant::_toQuery)
                .toList();

        if (repoInstanceQueries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(BoolQuery.of(bq -> bq.should(repoInstanceQueries)));
    }

    private BoolQuery getBoolQueryForRepo(String url, Instance instance) {
        List<Query> termsQueries = Stream.of(
                        termsQuery("repoUrl", url),
                        termsQuery("instance", instance.name()))
                .map(QueryVariant::_toQuery)
                .toList();

        return BoolQuery.of(bq -> bq.must(termsQueries));
    }


    private List<TermsQuery> getQueriesForParams(Set<String> types, Set<String> themes, Set<String> rightsHolder) {
        return Map.of("type", types, "themes", themes, "agencyId", rightsHolder).entrySet().stream()
                .filter(e -> nonNull(e.getValue()))
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> termsQuery(e.getKey(), e.getValue()))
                .toList();
    }

    private TermsQuery termsQuery(String field, String value) {
        return termsQuery(field, Set.of(value));
    }


    private TermsQuery termsQuery(String field, Set<String> values) {
        return TermsQuery.of(t -> t.field(field).terms(
                terms -> terms.value(values.stream()
                        .filter(Objects::nonNull)
                        .map(FieldValue::of)
                        .toList())));
    }

    public Optional<SemanticAssetMetadata> findByIri(String iri) {
        List<Query> queries = new ArrayList<>();
        queries.add(termQuery("iri", iri)._toQuery());
        getConditionForInstances()
                .map(QueryVariant::_toQuery)
                .ifPresent(queries::add);
        NativeQuery query = NativeQuery.builder()
                .withQuery(BoolQuery.of(bq -> bq.must(queries))._toQuery())
                .build();
        try {
            SearchHits<SemanticAssetMetadata> search = esOps.search(query, SemanticAssetMetadata.class);
            return search.get().findFirst().map(SearchHit::getContent);
        } catch (Exception e) {
            log.error("Error while searching for asset with iri: {}", iri, e);
        }
        return Optional.empty();
    }

    public long deleteByRepoUrl(String repoUrl, Instance instance) {
        return esOps.delete(
                DeleteQuery.builder(
                                NativeQuery.builder()
                                        .withQuery(getBoolQueryForRepo(repoUrl, instance)._toQuery())
                                        .build())
                        .build(),
                SemanticAssetMetadata.class).getDeleted();
    }

    public void save(SemanticAssetMetadata metadata) {
        esOps.save(metadata);
    }

    public List<SemanticAssetMetadata> findVocabulariesForRepoUrl(String repoUrl, Instance instance) {
        BoolQuery boolQuery = BoolQuery.of(
                bq -> bq.must(
                        List.of(
                                termQuery("repoUrl", repoUrl)._toQuery(),
                                termsQuery("instance", instance.name())._toQuery(),
                                termQuery("type", CONTROLLED_VOCABULARY.name())._toQuery())));

        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery._toQuery()).build();

        SearchHits<SemanticAssetMetadata> hits = esOps.search(query, SemanticAssetMetadata.class);
        return hits.get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
