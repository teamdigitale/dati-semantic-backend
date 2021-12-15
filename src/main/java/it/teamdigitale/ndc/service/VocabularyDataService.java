package it.teamdigitale.ndc.service;

import it.teamdigitale.ndc.controller.exception.VocabularyDataNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.teamdigitale.ndc.gen.dto.VocabularyData;
import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.model.ModelBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VocabularyDataService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public VocabularyDataService(
            ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public VocabularyData getData(VocabularyIdentifier vocabularyIdentifier, Pageable pageable) {
        String indexName = vocabularyIdentifier.getIndexName();
        if (exists(indexName)) {
            Query findAll = Query.findAll().setPageable(pageable);
            SearchHits<Map> results = elasticsearchOperations.search(findAll, Map.class, IndexCoordinates.of(indexName));

            List<Map<String, String>> data = results.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(m -> new HashMap<String, String>(m))
                    .collect(Collectors.toList());

            return ModelBuilder.vocabularyDataBuilder()
                    .totalResults(results.getTotalHits())
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .data(data)
                    .build();
        } else {
            log.error("Controlled Vocabulary not found for {}", vocabularyIdentifier);
            throw new VocabularyDataNotFoundException(indexName);
        }
    }

    public void indexData(VocabularyIdentifier vocabularyIdentifier,
                          CsvParser.CsvData data) {
        String indexName = vocabularyIdentifier.getIndexName();
        ensureCleanIndex(indexName);
        elasticsearchOperations.save(data.getRecords(), IndexCoordinates.of(indexName));
    }

    public void dropIndex(VocabularyIdentifier vocabularyIdentifier) {
        elasticsearchOperations.indexOps(IndexCoordinates.of(vocabularyIdentifier.getIndexName())).delete();
    }

    @SneakyThrows
    private void ensureCleanIndex(String indexName) {
        if (exists(indexName)) {
            elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).delete();
        }
        elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).create();
    }

    @SneakyThrows
    private boolean exists(String indexName) {
        return elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).exists();
    }
}