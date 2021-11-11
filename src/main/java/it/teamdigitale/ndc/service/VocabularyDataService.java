package it.teamdigitale.ndc.service;

import java.util.List;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<JSONObject> getData(String index) {
        NativeSearchQuery query = new NativeSearchQueryBuilder().build();
        SearchHits<JSONObject> results = elasticsearchOperations.search(query,
                JSONObject.class,
                IndexCoordinates.of(index));

        return results.getSearchHits()
                .stream().map(searchHits -> searchHits.getContent()).collect(Collectors.toList());
    }

}
