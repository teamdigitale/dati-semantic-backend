package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.exception.VocabularyDataNotFoundException;
import it.gov.innovazione.ndc.controller.exception.VocabularyItemNotFoundException;
import it.gov.innovazione.ndc.gen.dto.VocabularyData;
import it.gov.innovazione.ndc.harvester.csv.CsvParser;
import it.gov.innovazione.ndc.model.Builders;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        requireIndex(vocabularyIdentifier, indexName);

        Query findAll = Query.findAll().setPageable(pageable);
        SearchHits<Map> results = elasticsearchOperations.search(findAll, Map.class, IndexCoordinates.of(indexName));

        List<Map<String, String>> data = results.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(m -> new HashMap<String, String>(m))
                .collect(Collectors.toList());

        return Builders.vocabularyData()
                .totalResults((int) results.getTotalHits())
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize())
                .data(data)
                .build();
    }

    private void requireIndex(VocabularyIdentifier vocabularyIdentifier, String indexName) {
        if (!exists(indexName)) {
            log.error("Controlled Vocabulary not found for {}", vocabularyIdentifier);
            throw new VocabularyDataNotFoundException(indexName);
        }
    }

    public void indexData(VocabularyIdentifier vocabularyIdentifier,
                          CsvParser.CsvData data) {
        String indexName = vocabularyIdentifier.getIndexName();
        ensureCleanIndex(indexName);
        String idName = data.getIdName();

        List<IndexQuery> indexQueries = data.getRecords().stream()
                .map(r -> buildIndexQuery(idName, r))
                .collect(Collectors.toList());

        elasticsearchOperations.bulkIndex(indexQueries, IndexCoordinates.of(indexName));
    }

    private IndexQuery buildIndexQuery(String idName, Map<String, String> record) {
        String id = record.get(idName);
        return new IndexQueryBuilder()
                .withId(id)
                .withObject(record)
                .build();
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

    public Map<String, String> getItem(VocabularyIdentifier vocabularyIdentifier, String id) {
        String indexName = vocabularyIdentifier.getIndexName();
        requireIndex(vocabularyIdentifier, indexName);

        Map result = elasticsearchOperations.get(id, Map.class, IndexCoordinates.of(indexName));

        if (result == null) {
            throw new VocabularyItemNotFoundException(indexName, id);
        }
        return result;
    }
}