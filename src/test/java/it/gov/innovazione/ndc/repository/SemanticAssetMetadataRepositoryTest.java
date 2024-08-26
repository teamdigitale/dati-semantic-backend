package it.gov.innovazione.ndc.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.service.InstanceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticAssetMetadataRepositoryTest {
    @Mock
    private ElasticsearchOperations esOps;
    @Mock
    private InstanceManager instanceManager;

    @Mock
    private SearchHits<SemanticAssetMetadata> searchHits;
    @Mock
    private SearchPage<SemanticAssetMetadata> searchPage;

    @InjectMocks
    private SemanticAssetMetadataRepository repository;

    @Test
    void shouldFindById() {
        when(esOps.get("http://www.example.org/asset/1", SemanticAssetMetadata.class))
            .thenReturn(SemanticAssetMetadata.builder()
                .iri("http://www.example.org/asset/1")
                .build());

        Optional<SemanticAssetMetadata> asset =
            repository.findByIri("http://www.example.org/asset/1");

        assertThat(asset).isPresent();
        assertThat(asset.get().getIri()).isEqualTo("http://www.example.org/asset/1");
    }

    @Test
    void shouldNotFindById() {
        when(esOps.get("http://www.example.org/asset/1", SemanticAssetMetadata.class))
            .thenReturn(null);

        Optional<SemanticAssetMetadata> asset =
            repository.findByIri("http://www.example.org/asset/1");

        assertThat(asset).isEmpty();
    }

    @Test
    void shouldDeleteByIri() {
        ArgumentCaptor<DeleteQuery> captor = ArgumentCaptor.forClass(DeleteQuery.class);

        ByQueryResponse deletedResponse = ByQueryResponse.builder()
            .withDeleted(1L)
            .build();

        when(esOps.delete(captor.capture(), any(Class.class))).thenReturn(deletedResponse);

        long deleteCount = repository.deleteByRepoUrl("someRepoUrl", Instance.PRIMARY);

        assertThat(deleteCount).isEqualTo(1);
        Query query = captor.getValue().getQuery();
        assertNotNull(query);
        assertEquals("Query: {\"bool\":{\"must\":[{\"terms\":{\"repoUrl\":[\"someRepoUrl\"]}},{\"terms\":{\"instance\":[\"PRIMARY\"]}}]}}",
                Optional.of(query)
                        .map(q -> (NativeQuery) q)
                        .map(NativeQuery::getQuery)
                        .map(Object::toString)
                        .orElse(null));
    }

    @Test
    void shouldSave() {
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder()
            .iri("http://www.example.org/asset/1")
            .build();

        repository.save(metadata);

        verify(esOps).save(metadata);
    }

    @Test
    void shouldSearchUsingQueryStringAndFiltersAndPagination() {
        when(instanceManager.getCurrentInstances()).thenReturn(List.of());

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);

        when(esOps.search(captor.capture(), any(Class.class))).thenReturn(searchHits);

        SearchPage<SemanticAssetMetadata> searchResult =
                repository.search("query", Set.of("TYPE1"), Set.of("THEME1"), Collections.emptySet(), PageRequest.of(0, 10));

        assertThat(searchResult.getSearchHits()).isEqualTo(searchHits);

        NativeQuery query = captor.getValue();
        assertNotNull(query);

        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> must =
                Optional.of(query)
                        .map(NativeQuery::getQuery)
                        .map(co.elastic.clients.elasticsearch._types.query_dsl.Query::_get)
                        .map(BoolQuery.class::cast)
                        .map(BoolQuery::must)
                        .orElse(null);

        assertThat(must).hasSize(3);

        Stream.of(
                "Query: {\"match\":{\"searchableText\":{\"query\":\"query\"}}}",
                "Query: {\"terms\":{\"type\":[\"TYPE1\"]}}",
                "Query: {\"terms\":{\"themes\":[\"THEME1\"]}}")
                .forEach(qs -> assertTrue(must.stream().map(Object::toString).anyMatch(qs::equals)));
    }

    @Test
    void shouldSearchWithoutFiltersAndSearchText() {
        when(instanceManager.getCurrentInstances()).thenReturn(List.of());

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);

        when(esOps.search(captor.capture(), any(Class.class))).thenReturn(searchHits);

        SearchPage<SemanticAssetMetadata> searchResult =
                repository.search("", Set.of(), Set.of(), Collections.emptySet(), PageRequest.of(0, 10));

        assertThat(searchResult.getSearchHits()).isEqualTo(searchHits);
        NativeQuery query = captor.getValue();
        assertNotNull(query);
        assertThat(query.getQuery().toString()).isEqualTo("Query: {\"bool\":{\"must\":[]}}");
    }
}
