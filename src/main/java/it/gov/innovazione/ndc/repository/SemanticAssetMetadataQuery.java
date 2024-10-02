package it.gov.innovazione.ndc.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import it.gov.innovazione.ndc.harvester.model.Instance;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class SemanticAssetMetadataQuery {
    static BoolQuery getBoolQueryForRepo(String url, Instance instance) {
        List<Query> termsQueries = Stream.of(
                        termsQuery("repoUrl", url),
                        termsQuery("instance", instance.name()))
                .map(QueryVariant::_toQuery)
                .toList();

        return BoolQuery.of(bq -> bq.must(termsQueries));
    }

    static TermsQuery termsQuery(String field, String value) {
        return termsQuery(field, Set.of(value));
    }

    static TermsQuery termsQuery(String field, Set<String> values) {
        return TermsQuery.of(t -> t.field(field).terms(
                terms -> terms.value(values.stream()
                        .filter(Objects::nonNull)
                        .map(FieldValue::of)
                        .toList())));
    }

    static DeleteQuery.Builder getDeleteQueryBuilder(String repoUrl, Instance instance) {
        return DeleteQuery.builder(
                NativeQuery.builder()
                        .withQuery(getBoolQueryForRepo(repoUrl, instance)._toQuery())
                        .build());
    }
}
