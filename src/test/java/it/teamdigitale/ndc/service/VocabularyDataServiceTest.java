package it.teamdigitale.ndc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.dto.VocabularyDataDto;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

@ExtendWith(MockitoExtension.class)
public class VocabularyDataServiceTest {
    @Mock
    ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    VocabularyDataService vocabularyDataService;

    @Test
    void shouldFetchDataFromElasticSearchForGivenIndex() {
        Map<String, String> data = Map.of("key", "val");
        SearchHits<Map> searchHits = mock(SearchHits.class);
        SearchHit searchHit = mock(SearchHit.class);
        String expectedIndex = "person-title";
        String agencyId = "agid";

        when(searchHits.getSearchHits()).thenReturn(Arrays.asList(searchHit));
        when(searchHit.getContent()).thenReturn(data);
        when(elasticsearchOperations.search(any(Query.class), eq(Map.class),
            any(IndexCoordinates.class))).thenReturn(searchHits);
        VocabularyDataDto actual = vocabularyDataService.getData(agencyId, expectedIndex, 0, 10);
        ArgumentCaptor<IndexCoordinates> indexCaptor =
            ArgumentCaptor.forClass(IndexCoordinates.class);
        ArgumentCaptor<Query> queryArgumentCaptor = ArgumentCaptor.forClass(Query.class);
        verify(elasticsearchOperations).search(queryArgumentCaptor.capture(), eq(Map.class),
            indexCaptor.capture());
        String actualIndex = indexCaptor.getValue().getIndexName();

        assertThat(queryArgumentCaptor.getValue().getPageable().getPageNumber()).isEqualTo(0);
        assertThat(queryArgumentCaptor.getValue().getPageable().getPageSize()).isEqualTo(10);
        assertThat("agid." + expectedIndex).isEqualTo(actualIndex);
        assertThat(Arrays.asList(data)).isEqualTo(actual.getData());
    }
}
