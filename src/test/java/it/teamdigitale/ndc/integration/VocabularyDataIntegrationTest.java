package it.teamdigitale.ndc.integration;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.restassured.response.Response;
import it.teamdigitale.ndc.config.ElasticsearchClientConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class VocabularyDataIntegrationTest {

    private static final int ELASTICSEARCH_PORT = 9200;
    private static final String CLUSTER_NAME = "cluster.name";
    public static final String indexName = "agency-vocab";
    private static final DockerImageName ELASTICSEARCH_IMAGE = parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag("7.10.0");

    @LocalServerPort
    private int port;

    @Autowired
    ElasticsearchClientConfig clientConfig;

    @Container
    private static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
        .withReuse(true)
        .withExposedPorts(ELASTICSEARCH_PORT)
        .withEnv("discovery.type", "single-node")
        .withEnv(CLUSTER_NAME, "elasticsearch")
        .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(9200), new ExposedPort(9200)))
        ));

    @BeforeAll
    private static void setup() throws IOException {
        setupIndexData();
    }

    @Test
    void shouldGetControlledVocabularyData() {
        // when
        Response response = given()
                .when()
                .get(String.format("http://localhost:%d/vocabularies/agency/vocab?page_number=1&page_size=2", port));

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
                .get(String.format("http://localhost:%d/vocabularies/agency/vocab?page_number=0&page_size=2", port));

        response.then()
                .statusCode(400);

    }

    @Test
    void shouldValidatePageSizeWhileGettingControlledVocabularyData() throws IOException {

        Response response = given()
                .when()
                .get(String.format("http://localhost:%d/vocabularies/agency/vocab?page_number=1&page_size=0", port));

        response.then()
                .statusCode(400);

    }

    @Test
    void shouldValidateMaxPageSizeWhileGettingControlledVocabularyData() throws IOException {

        Response response = given()
                .when()
                .get(String.format("http://localhost:%d/vocabularies/agency/vocab?page_number=1&page_size=250", port));

        response.then()
                .statusCode(400);

    }

    private static void setupIndexData() throws IOException {
        RestClient client = RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress())).build();

        //create index
        Request request = new Request("PUT", "/" + indexName);
        HttpEntity entity = new NStringEntity("{}", ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        client.performRequest(request);

        //add data
        Request dataRequest1 = new Request("POST", "/" + indexName + "/_doc");
        HttpEntity dataEntity1 = new NStringEntity("{\"name\":\"Rob\"}", ContentType.APPLICATION_JSON);
        dataRequest1.setEntity(dataEntity1);
        client.performRequest(dataRequest1);

        Request dataRequest2 = new Request("POST", "/" + indexName + "/_doc");
        HttpEntity dataEntity2 = new NStringEntity("{\"name\":\"Sam\"}", ContentType.APPLICATION_JSON);
        dataRequest2.setEntity(dataEntity2);
        client.performRequest(dataRequest2);

        Request dataRequest3 = new Request("POST", "/" + indexName + "/_doc");
        HttpEntity dataEntity3 = new NStringEntity("{\"name\":\"Peter\"}", ContentType.APPLICATION_JSON);
        dataRequest3.setEntity(dataEntity3);
        client.performRequest(dataRequest3);
    }
}
