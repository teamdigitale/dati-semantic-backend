package it.teamdigitale.ndc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.controller.OffsetBasedPageRequest;
import it.teamdigitale.ndc.controller.dto.VocabularyDataDto;
import it.teamdigitale.ndc.controller.exception.VocabularyDataNotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

@ExtendWith(MockitoExtension.class)
public class VocabularyDataServiceTest {
    @Mock
    ElasticsearchOperations elasticsearchOperations;

    @Mock
    IndexOperations indexOperations;

    @InjectMocks
    VocabularyDataService vocabularyDataService;

    @Test
    void shouldFetchDataFromElasticSearchForGivenIndex() {
        SearchHit<Map> hit = mock(SearchHit.class);
        SearchHits<Map> hits = mock(SearchHits.class);

        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(hits.getTotalHits()).thenReturn(1L);
        when(hit.getContent()).thenReturn(Map.of("key", "value"));
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
            .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(captor.capture(), any(Class.class),
            any(IndexCoordinates.class))).thenReturn(hits);

        VocabularyDataDto data = vocabularyDataService.getData("agid", "testKeyConcept", OffsetBasedPageRequest.of(2, 10));

        assertThat(data.getData()).containsExactlyInAnyOrder(Map.of("key", "value"));
        assertThat(data.getTotalResults()).isEqualTo(1L);
        assertThat(data.getOffset()).isEqualTo(2);
        assertThat(data.getLimit()).isEqualTo(10);
        IndexCoordinates indexCoordinates = IndexCoordinates.of("agid.testkeyconcept");
        verify(elasticsearchOperations).indexOps(indexCoordinates);
        verify(indexOperations).exists();
        Query actualQuery = captor.getValue();
        verify(elasticsearchOperations).search(actualQuery, Map.class, indexCoordinates);
        assertThat(actualQuery.getPageable().getOffset()).isEqualTo(2L);
        assertThat(actualQuery.getPageable().getPageSize()).isEqualTo(10);
        assertThat(actualQuery.getFields()).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenIndexDoesNotExists() {
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
            .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);
        IndexCoordinates indexCoordinates = IndexCoordinates.of("agid.testkeyconcept");

        assertThatThrownBy(() -> vocabularyDataService.getData("agid", "testKeyConcept", OffsetBasedPageRequest.of(0, 10)))
            .isInstanceOf(VocabularyDataNotFoundException.class)
            .hasMessage("Unable to find vocabulary data for : agid.testkeyconcept");
        verify(elasticsearchOperations).indexOps(indexCoordinates);
        verify(indexOperations).exists();
    }

    @Test
    void shouldIndexTheNewDataWhenIndexIsAlreadyPresent() {
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
            .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);

        vocabularyDataService.indexData("agid", "testKeyConcept", List.of(Map.of("key", "value")));

        verify(elasticsearchOperations, times(3)).indexOps(
            IndexCoordinates.of("agid.testkeyconcept"));
        verify(indexOperations).exists();
        verify(indexOperations).delete();
        verify(indexOperations).create();
        verify(elasticsearchOperations).save(List.of(Map.of("key", "value")),
            IndexCoordinates.of("agid.testkeyconcept"));
    }

    @Test
    void shouldIndexTheNewDataWhenIndexIsNotPresent() {
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
            .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);

        vocabularyDataService.indexData("agid", "testKeyConcept", List.of(Map.of("key", "value")));

        verify(elasticsearchOperations, times(2)).indexOps(
            IndexCoordinates.of("agid.testkeyconcept"));
        verify(indexOperations).exists();
        verify(indexOperations, times(0)).delete();
        verify(indexOperations).create();
        verify(elasticsearchOperations).save(List.of(Map.of("key", "value")),
            IndexCoordinates.of("agid.testkeyconcept"));
    }
}
