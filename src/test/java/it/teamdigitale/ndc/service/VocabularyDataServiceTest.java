package it.teamdigitale.ndc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import it.teamdigitale.ndc.dto.VocabularyDataDto;
import org.json.simple.JSONObject;
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
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;

@ExtendWith(MockitoExtension.class)
public class VocabularyDataServiceTest {
    @Mock
    ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    VocabularyDataService vocabularyDataService;

    @Test
    void shouldFetchDataFromElasticSearchForGivenIndex() {
        JSONObject data = new JSONObject();
        SearchHits<JSONObject> searchHits = mock(SearchHits.class);
        SearchHit searchHit = mock(SearchHit.class);
        String expectedIndex = "person-title";
        String agencyId = "agid";

        when(searchHits.getSearchHits()).thenReturn(Arrays.asList(searchHit));
        when(searchHit.getContent()).thenReturn(data);
        when(elasticsearchOperations.search(any(NativeSearchQuery.class),
                eq(JSONObject.class),
                any(IndexCoordinates.class)))
                .thenReturn(searchHits);
        VocabularyDataDto actual = vocabularyDataService.getData(agencyId, expectedIndex, 0, 10);
        ArgumentCaptor<IndexCoordinates> indexCaptor = ArgumentCaptor.forClass(IndexCoordinates.class);
        ArgumentCaptor<NativeSearchQuery> nativeSearchQueryArgumentCaptor = ArgumentCaptor.forClass(NativeSearchQuery.class);
        verify(elasticsearchOperations).search(nativeSearchQueryArgumentCaptor.capture(),
                eq(JSONObject.class),
                indexCaptor.capture());
        String actualIndex = indexCaptor.getValue().getIndexName();

        assertEquals(nativeSearchQueryArgumentCaptor.getValue().getPageable().getPageNumber(), 0);
        assertEquals(nativeSearchQueryArgumentCaptor.getValue().getPageable().getPageSize(), 10);
        assertEquals("agid-" + expectedIndex, actualIndex);
        assertEquals(Arrays.asList(data), actual.getData());
    }
}
