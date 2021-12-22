package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.OffsetBasedPageRequest;
import it.gov.innovazione.ndc.controller.exception.VocabularyDataNotFoundException;
import it.gov.innovazione.ndc.controller.exception.VocabularyItemNotFoundException;
import it.gov.innovazione.ndc.gen.dto.VocabularyData;
import it.gov.innovazione.ndc.harvester.csv.CsvParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VocabularyDataServiceTest {
    public static final CsvParser.CsvData CSV_DATA = new CsvParser.CsvData(List.of(Map.of("key", "value")), "key");
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

        VocabularyData data = vocabularyDataService.getData(new VocabularyIdentifier("agid", "testKeyConcept"), OffsetBasedPageRequest.of(2, 10));

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

        assertThatThrownBy(() -> vocabularyDataService.getData(new VocabularyIdentifier("agid", "testKeyConcept"), OffsetBasedPageRequest.of(0, 10)))
                .isInstanceOf(VocabularyDataNotFoundException.class)
                .hasMessage("Unable to find vocabulary data for : agid.testkeyconcept");
        verify(elasticsearchOperations).indexOps(indexCoordinates);
        verify(indexOperations).exists();
    }

    @Test
    void shouldThrowExceptionWhenItemDoesNotExists() {
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
                .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        IndexCoordinates indexCoordinates = IndexCoordinates.of("agid.testkeyconcept");
        when(elasticsearchOperations.get(eq("bugs-bunny"), any(), eq(indexCoordinates))).thenReturn(null);

        VocabularyIdentifier vocabularyIdentifier = new VocabularyIdentifier("agid", "testKeyConcept");
        assertThatThrownBy(() -> vocabularyDataService.getItem(vocabularyIdentifier, "bugs-bunny"))
                .isInstanceOf(VocabularyItemNotFoundException.class)
                .hasMessageContaining("bugs-bunny")
                .hasMessageContaining("agid.testkeyconcept");
    }

    @Test
    void shouldIndexTheNewDataWhenIndexIsAlreadyPresent() {
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
                .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);

        vocabularyDataService.indexData(new VocabularyIdentifier("agid", "testKeyConcept"), CSV_DATA);

        verify(elasticsearchOperations, times(3)).indexOps(
                IndexCoordinates.of("agid.testkeyconcept"));
        verify(indexOperations).exists();
        verify(indexOperations).delete();
        verify(indexOperations).create();
        verify(elasticsearchOperations).bulkIndex(anyList(), eq(IndexCoordinates.of("agid.testkeyconcept")));
    }

    @Test
    void shouldIndexTheNewDataWhenIndexIsNotPresent() {
        IndexCoordinates indexCoordinates = IndexCoordinates.of("agid.testkeyconcept");
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class)))
                .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);

        vocabularyDataService.indexData(new VocabularyIdentifier("agid", "testKeyConcept"), CSV_DATA);

        verify(elasticsearchOperations, times(2)).indexOps(indexCoordinates);
        verify(indexOperations).exists();
        verify(indexOperations, never()).delete();
        verify(indexOperations).create();
        verify(elasticsearchOperations).bulkIndex(anyList(), eq(indexCoordinates));
    }
}
