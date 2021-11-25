package it.teamdigitale.ndc.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.when;
import static org.testcontainers.utility.DockerImageName.parse;

import io.restassured.response.Response;
import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
import org.springframework.boot.test.mock.mockito.MockBean;
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
    private static final String cloneDirectory = "src/test/resources/testdata";
    private static final String ttlPath = "src/test/resources/testdata/cv.ttl";
    private static final String csvPath = "src/test/resources/testdata/cv.csv";

    @LocalServerPort
    private int port;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    HarvesterService harvesterService;

    @MockBean
    AgencyRepositoryService agencyRepositoryService;

    @MockBean
    TripleStoreRepository tripleStoreRepository;

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
    void shouldHarvestAndGetControlledVocabularyData() throws IOException {
        // when
        Path cloneDir = Path.of(cloneDirectory);
        String repositoryUrl = "testRepoURL";
        when(agencyRepositoryService.cloneRepo(repositoryUrl)).thenReturn(cloneDir);
        when(agencyRepositoryService.getControlledVocabularyPaths(cloneDir))
            .thenReturn(List.of(CvPath.of(ttlPath, csvPath)));

        harvesterService.harvest(repositoryUrl);

        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/agid/testVocabulary", port));

        // then
        response.then()
            .statusCode(200)
            .body("totalResults", equalTo(2))
            .body("pageNumber", equalTo(1))
            .body("data.size()", equalTo(2))
            .body("data[0].code_level_1", equalTo("3.0"));
    }

    @Test
    void shouldValidatePageNumberWhileGettingControlledVocabularyData() {

        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/agency/vocab?page_number=0&page_size=2", port));

        response.then()
            .statusCode(400);

    }

    @Test
    void shouldValidatePageSizeWhileGettingControlledVocabularyData() {

        Response response = given()
            .when()
            .get(String.format(
                "http://localhost:%d/vocabularies/agency/vocab?page_number=1&page_size=0", port));

        response.then()
            .statusCode(400);

    }

    @Test
    void shouldValidateMaxPageSizeWhileGettingControlledVocabularyData() {

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
