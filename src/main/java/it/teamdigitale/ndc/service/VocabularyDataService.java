package it.teamdigitale.ndc.service;

import java.util.List;
import java.util.stream.Collectors;

import it.teamdigitale.ndc.dto.VocabularyDataDto;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class VocabularyDataService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public VocabularyDataDto getData(String index, Integer pageNumber, Integer pageSize) {
        int currentPageNumber = 0;
        NativeSearchQuery query = new NativeSearchQueryBuilder().build();
        if (pageNumber != null && pageSize != null) {
            query.setPageable(PageRequest.of(pageNumber, pageSize));
        }
        SearchHits<JSONObject> results = elasticsearchOperations.search(query,
                JSONObject.class, IndexCoordinates.of(index));

        List<JSONObject> data = results.getSearchHits().stream()
                .map(searchHits -> searchHits.getContent())
                .collect(Collectors.toList());
        return new VocabularyDataDto(results.getTotalHits(), currentPageNumber, data);
    }

}