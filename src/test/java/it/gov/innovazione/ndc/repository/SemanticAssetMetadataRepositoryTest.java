package it.gov.innovazione.ndc.repository;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;

import java.util.Optional;
import java.util.Set;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;

@ExtendWith(MockitoExtension.class)
class SemanticAssetMetadataRepositoryTest {
    @Mock
    private ElasticsearchOperations esOps;

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
        ArgumentCaptor<NativeSearchQuery> captor = ArgumentCaptor.forClass(NativeSearchQuery.class);
        ByQueryResponse deletedResponse = ByQueryResponse.builder()
            .withDeleted(1L)
            .build();
        when(esOps.delete(captor.capture(), any(Class.class))).thenReturn(deletedResponse);

        long deleteCount = repository.deleteByRepoUrl("someRepoUrl");

        assertThat(deleteCount).isEqualTo(1);
        MatchQueryBuilder query = (MatchQueryBuilder) captor.getValue().getQuery();
        assert query != null;
        assertThat(query.fieldName()).isEqualTo("repoUrl");
        assertThat(query.value()).isEqualTo("someRepoUrl");
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
        ArgumentCaptor<NativeSearchQuery> captor = ArgumentCaptor.forClass(NativeSearchQuery.class);

        when(esOps.search(captor.capture(), any(Class.class))).thenReturn(searchHits);

        SearchPage<SemanticAssetMetadata> searchResult =
            repository.search("query", Set.of("TYPE1"), Set.of("THEME1"), rightsHolder, PageRequest.of(0, 10));

        assertThat(searchResult.getSearchHits()).isEqualTo(searchHits);
        BoolQueryBuilder query = (BoolQueryBuilder) captor.getValue().getQuery();
        assert query != null;
        assertThat(query.must().size()).isEqualTo(1);
        MatchQueryBuilder matchQuery = (MatchQueryBuilder) query.must().get(0);
        assertThat(matchQuery.fieldName()).isEqualTo("searchableText");
        assertThat(matchQuery.value()).isEqualTo("query");

        assertThat(query.filter().size()).isEqualTo(2);

        TermsQueryBuilder typeFilter = (TermsQueryBuilder) query.filter().get(0);
        assertThat(typeFilter.fieldName()).isEqualTo("type");
        assertThat(typeFilter.values()).containsExactlyInAnyOrder("TYPE1");

        TermsQueryBuilder themeFilter = (TermsQueryBuilder) query.filter().get(1);
        assertThat(themeFilter.fieldName()).isEqualTo("themes");
        assertThat(themeFilter.values()).containsExactlyInAnyOrder("THEME1");
    }

    @Test
    void shouldSearchWithoutFiltersAndSearchText() {
        ArgumentCaptor<NativeSearchQuery> captor = ArgumentCaptor.forClass(NativeSearchQuery.class);

        when(esOps.search(captor.capture(), any(Class.class))).thenReturn(searchHits);

        SearchPage<SemanticAssetMetadata> searchResult =
            repository.search("", Set.of(), Set.of(), rightsHolder, PageRequest.of(0, 10));

        assertThat(searchResult.getSearchHits()).isEqualTo(searchHits);
        BoolQueryBuilder query = (BoolQueryBuilder) captor.getValue().getQuery();
        assert query != null;
        assertThat(requireNonNull(query).must().size()).isEqualTo(1);
        assertThat(query.must().get(0)).isInstanceOf(MatchAllQueryBuilder.class);

        assertThat(query.filter().size()).isEqualTo(0);
    }
}
