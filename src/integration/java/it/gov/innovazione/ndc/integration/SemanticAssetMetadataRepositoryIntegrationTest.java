package it.gov.innovazione.ndc.integration;

import it.gov.innovazione.ndc.config.ElasticConfigurator;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.ArrayList;
import java.util.List;

import static it.gov.innovazione.ndc.integration.Containers.ELASTIC_PASSWORD;
import static it.gov.innovazione.ndc.integration.Containers.ELASTIC_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

public class SemanticAssetMetadataRepositoryIntegrationTest {
    private static final ElasticsearchContainer elastic = Containers.buildElasticsearchContainer();
    public static final int INDEX_COUNT = 2;
    public static final int ASSET_COUNT = 5;
    private static SemanticAssetMetadataRepository repository;
    private static ElasticsearchOperations elasticsearchOperations;

    @BeforeAll
    public static void beforeAll() {
        elastic.start();
        elasticsearchOperations = buildElasticsearchOps();
        repository = new SemanticAssetMetadataRepository(elasticsearchOperations, new InMemoryInstanceManager());
    }

    @NotNull
    private static ElasticsearchOperations buildElasticsearchOps() {
        RestClient client = new ElasticConfigurator(
                "localhost",
                elastic.getMappedPort(Containers.ELASTICSEARCH_PORT), "https",
                ELASTIC_USERNAME, ELASTIC_PASSWORD).client();

        return new ElasticsearchTemplate(
                ElasticsearchClients.createImperative(client));
    }

    @AfterAll
    public static void afterAll() {
        elastic.stop();
    }

    @Test
    void shouldIndexAndSearch() {
        elasticsearchOperations.indexOps(SemanticAssetMetadata.class).createWithMapping();

        List<SemanticAssetMetadata> entries = new ArrayList<>();

        for (int repoIndex = 1; repoIndex <= INDEX_COUNT; repoIndex++) {
            String repoName = String.format("http://repo%d", repoIndex);
            for (SemanticAssetType type : SemanticAssetType.values()) {
                for (int assetIndex = 1; assetIndex <= ASSET_COUNT; assetIndex++) {
                    String iri = String.format("%s/%s/asset%d", repoName, type, assetIndex);
                    SemanticAssetMetadata entry = SemanticAssetMetadata.builder()
                            .repoUrl(repoName)
                            .type(type)
                            .iri(iri)
                            .instance(Instance.PRIMARY.name())
                            .build();
                    entries.add(entry);
                }
            }
        }

        entries.forEach(repository::save);

        elasticsearchOperations.indexOps(SemanticAssetMetadata.class).refresh();

        List<SemanticAssetMetadata> vocabs = repository.findVocabulariesForRepoUrl("http://repo1", Instance.PRIMARY);
        assertThat(vocabs).hasSize(ASSET_COUNT);
    }
}
