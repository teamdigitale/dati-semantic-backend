package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.exception.VocabularyDataNotFoundException;
import it.gov.innovazione.ndc.controller.exception.VocabularyItemNotFoundException;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.gen.dto.VocabularyData;
import it.gov.innovazione.ndc.harvester.csv.CsvParser;
import it.gov.innovazione.ndc.integration.Containers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class VocabularyDataServiceIntegrationTest {
    private static final VocabularyIdentifier VOCABULARY_IDENTIFIER = new VocabularyIdentifier("agid", "testfacts");
    private static final ElasticsearchContainer elasticsearchContainer = Containers.buildElasticsearchContainer();

    @Autowired
    private VocabularyDataService vocabularyDataService;

    @Autowired
    private ElasticsearchOperations elasticOps;

    @MockBean
    private GithubService githubService;

    @MockBean
    private NdcEventPublisher eventPublisher;

    @DynamicPropertySource
    static void updateTestcontainersProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.rest.uris", elasticsearchContainer::getHttpHostAddress);
    }

    @BeforeAll
    public static void startContainers() {
        elasticsearchContainer.start();
    }

    @AfterAll
    public static void stopContainers() {
        elasticsearchContainer.stop();
    }

    @Test
    void shouldIndexDataThenDropIndex() {
        assertThatThrownBy(() -> vocabularyDataService.getData(VOCABULARY_IDENTIFIER, Pageable.ofSize(20)))
                .isInstanceOf(VocabularyDataNotFoundException.class);

        Map<String, String> martinRecord = Map.of("id", "martin", "name", "Martin Fowler");
        Map<String, String> kentRecord = Map.of("id", "kent", "name", "Kent Beck");
        CsvParser.CsvData data = new CsvParser.CsvData(List.of(kentRecord, martinRecord), "id");

        vocabularyDataService.indexData(VOCABULARY_IDENTIFIER, data);

        forceIndexFlush();

        VocabularyData searchResultAfterIndexing = vocabularyDataService.getData(VOCABULARY_IDENTIFIER, Pageable.ofSize(20));
        assertThat(searchResultAfterIndexing.getTotalResults()).isEqualTo(2);
        List<Map<String, String>> dataAfterIndexing = searchResultAfterIndexing.getData();
        assertThat(dataAfterIndexing).isEqualTo(data.getRecords());

        Map<String, String> item = vocabularyDataService.getItem(VOCABULARY_IDENTIFIER, "martin");
        assertThat(item).isEqualTo(martinRecord);
        assertThatThrownBy(() -> vocabularyDataService.getItem(VOCABULARY_IDENTIFIER, "ward"))
                .isInstanceOf(VocabularyItemNotFoundException.class);

        vocabularyDataService.dropIndex(VOCABULARY_IDENTIFIER);

        assertThatThrownBy(() -> vocabularyDataService.getData(VOCABULARY_IDENTIFIER, Pageable.ofSize(20)))
                .isInstanceOf(VocabularyDataNotFoundException.class);

        assertThatThrownBy(() -> vocabularyDataService.getItem(VOCABULARY_IDENTIFIER, "martin"))
                .isInstanceOf(VocabularyDataNotFoundException.class);
    }

    private void forceIndexFlush() {
        elasticOps.indexOps(IndexCoordinates.of(VOCABULARY_IDENTIFIER.getIndexName())).refresh();
    }

}
