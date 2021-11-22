package it.teamdigitale.ndc.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.testcontainers.utility.DockerImageName.parse;

import io.restassured.response.Response;
import it.teamdigitale.ndc.dto.VocabularyDataDto;
import it.teamdigitale.ndc.service.VocabularyDataService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class VocabularyDataIntegrationTest {

    private static final int ELASTICSEARCH_PORT = 9200;
    private static final String CLUSTER_NAME = "cluster.name";
    public static final String indexName = "agency.vocab";
    private static final DockerImageName ELASTICSEARCH_IMAGE =
        parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag("7.12.0");

    @LocalServerPort
    private int port;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    private static ElasticsearchContainer elasticsearchContainer =
        new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withReuse(true)
            .withExposedPorts(ELASTICSEARCH_PORT)
            .withEnv("discovery.type", "single-node")
            .withEnv(CLUSTER_NAME, "elasticsearch");

    @DynamicPropertySource
    static void updateTestcontainersProperties(DynamicPropertyRegistry registry) {
        registry.add("elasticsearch.port",
            () -> elasticsearchContainer.getMappedPort(ELASTICSEARCH_PORT));
    }

    @BeforeAll
    private static void setup() throws IOException {
        elasticsearchContainer.start();
        setupIndexData();
    }

    @Test
    void shouldGetControlledVocabularyData() {
        // when
        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/agency/vocab?page_number=1&page_size=2", port));

        // then
        response.then()
            .statusCode(200)
            .body("totalResults", equalTo(3))
            .body("pageNumber", equalTo(1))
            .body("data.size()", equalTo(2))
            .body("data[0].name", equalTo("Rob"))
            .body("data[1].name", equalTo("Sam"));

    }

    @Test
    void shouldValidatePageNumberWhileGettingControlledVocabularyData() throws IOException {

        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/agency/vocab?page_number=0&page_size=2", port));

        response.then()
            .statusCode(400);

    }

    @Test
    void shouldValidatePageSizeWhileGettingControlledVocabularyData() throws IOException {

        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/agency/vocab?page_number=1&page_size=0", port));

        response.then()
            .statusCode(400);

    }

    @Test
    void shouldValidateMaxPageSizeWhileGettingControlledVocabularyData() throws IOException {

        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/agency/vocab?page_number=1&page_size=250", port));

        response.then()
            .statusCode(400);

    }

    @Test
    void shouldReturnNotFoundErrorWhenDataIsNotPresent() {
        // when
        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/wrong/wrong?page_number=1&page_size=2", port));

        // then
        response.then()
            .statusCode(404)
            .body("message", equalTo("Unable to find vocabulary data for : wrong.wrong"));
    }

    /**
     * We do not have any API to invoke this code, hence this way is used.
     */
    @Test
    void shouldCreateNewIndexAndSaveTheData() throws InterruptedException {
        VocabularyDataService vocabularyDataService =
            new VocabularyDataService(elasticsearchOperations, restHighLevelClient);

        vocabularyDataService.indexData("rightsHolder", "keyConcept",
            List.of(Map.of("key", "val")));

        //TODO find a better way
        Thread.sleep(3000);
        VocabularyDataDto data = vocabularyDataService.getData("rightsHolder", "keyConcept", 0, 10);
        assertThat(data.getData()).hasSize(1);
        assertThat(data.getData().get(0).get("key")).isEqualTo("val");
    }

    private static void setupIndexData() throws IOException {
        RestClient client =
            RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress()))
                .build();

        //delete index
        Request deleteRequest = new Request("DELETE", "/" + indexName);
        try {
            client.performRequest(deleteRequest);
        } catch (Exception ignored) {
            System.out.println("Index does not exist");
        }

        //create index
        Request request = new Request("PUT", "/" + indexName);
        HttpEntity entity = new NStringEntity("{}", ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        client.performRequest(request);

        //add data
        Request dataRequest1 = new Request("POST", "/" + indexName + "/_doc");
        HttpEntity dataEntity1 =
            new NStringEntity("{\"name\":\"Rob\"}", ContentType.APPLICATION_JSON);
        dataRequest1.setEntity(dataEntity1);
        client.performRequest(dataRequest1);

        Request dataRequest2 = new Request("POST", "/" + indexName + "/_doc");
        HttpEntity dataEntity2 =
            new NStringEntity("{\"name\":\"Sam\"}", ContentType.APPLICATION_JSON);
        dataRequest2.setEntity(dataEntity2);
        client.performRequest(dataRequest2);

        Request dataRequest3 = new Request("POST", "/" + indexName + "/_doc");
        HttpEntity dataEntity3 =
            new NStringEntity("{\"name\":\"Peter\"}", ContentType.APPLICATION_JSON);
        dataRequest3.setEntity(dataEntity3);
        client.performRequest(dataRequest3);
    }
}
