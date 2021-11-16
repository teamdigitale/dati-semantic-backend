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

    public VocabularyDataDto getData(String index, Integer pageIndex, Integer pageSize) {
        NativeSearchQuery query = new NativeSearchQueryBuilder().build();
        query.setPageable(PageRequest.of(pageIndex, pageSize));

        SearchHits<JSONObject> results = elasticsearchOperations.search(query,
                JSONObject.class, IndexCoordinates.of(index));

        List<JSONObject> data = results.getSearchHits().stream()
                .map(searchHits -> searchHits.getContent())
                .collect(Collectors.toList());
        return new VocabularyDataDto(results.getTotalHits(), pageIndex, data);
    }

}