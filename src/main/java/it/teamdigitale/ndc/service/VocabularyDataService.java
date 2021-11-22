package it.teamdigitale.ndc.service;

import static org.elasticsearch.client.RequestOptions.DEFAULT;

import it.teamdigitale.ndc.controller.exception.VocabularyDataNotFoundException;
import it.teamdigitale.ndc.dto.VocabularyDataDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VocabularyDataService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final RestHighLevelClient esClient;

    @Autowired
    public VocabularyDataService(
        ElasticsearchOperations elasticsearchOperations, RestHighLevelClient esClient) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.esClient = esClient;
    }

    public VocabularyDataDto getData(String rightsHolder, String keyConcept, Integer pageIndex,
                                     Integer pageSize) {
        String index = String.join(".", rightsHolder, keyConcept).toLowerCase();
        if (exists(index)) {
            Query findAll = Query.findAll().setPageable(PageRequest.of(pageIndex, pageSize));
            SearchHits<Map> results =
                elasticsearchOperations.search(findAll, Map.class, IndexCoordinates.of(index));

            List<Map> data = results.getSearchHits().stream()
                .map(searchHits -> searchHits.getContent())
                .collect(Collectors.toList());
            return new VocabularyDataDto(results.getTotalHits(), pageIndex, data);
        } else {
            log.error("Controlled Vocabulary not found for {}/{}", rightsHolder, keyConcept);
            throw new VocabularyDataNotFoundException(index);
        }
    }

    public void indexData(String rightsHolder, String keyConcept,
                          List<Map<String, String>> data) {
        String indexName = String.join(".", rightsHolder, keyConcept).toLowerCase();
        ensureCleanIndex(indexName);
        elasticsearchOperations.save(data, IndexCoordinates.of(indexName));
    }

    @SneakyThrows
    private void ensureCleanIndex(String indexName) {
        if (exists(indexName)) {
            esClient.indices().delete(new DeleteIndexRequest(indexName), DEFAULT);
        }
        esClient.indices().create(new CreateIndexRequest(indexName), DEFAULT);
    }

    @SneakyThrows
    private boolean exists(String indexName) {
        return esClient.indices().exists(new GetIndexRequest(indexName), DEFAULT);

    }
}