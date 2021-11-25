package it.teamdigitale.ndc.integration;

import io.restassured.response.Response;
import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of;
import static org.testcontainers.utility.DockerImageName.parse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class VocabularyHarvestIntegrationTest {

    private static final ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
        parse("docker.elastic.co/elasticsearch/elasticsearch").withTag("7.12.0"))
        .withReuse(true)
        .withExposedPorts(9200)
        .withEnv("discovery.type", "single-node")
        .withEnv("cluster.name", "elasticsearch");

    @LocalServerPort
    private int port;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    HarvesterService harvesterService;

    @MockBean
    AgencyRepositoryService agencyRepositoryService;

    @MockBean
    TripleStoreRepository tripleStoreRepository;

    @BeforeAll
    public static void beforeAll() {
        elasticsearchContainer.start();
    }

    @DynamicPropertySource
    static void updateTestcontainersProperties(DynamicPropertyRegistry registry) {
        registry.add("elasticsearch.port", () -> elasticsearchContainer.getMappedPort(9200));
    }

    @Test
    void shouldHarvestControlledVocabularySemanticAssest() throws IOException {
        //given
        String dir = "src/test/resources/testdata";
        Path cloneDir = Path.of(dir);
        String repositoryUrl = "testRepoURL";
        when(agencyRepositoryService.cloneRepo(repositoryUrl)).thenReturn(cloneDir);
        when(agencyRepositoryService.getControlledVocabularyPaths(cloneDir)).thenReturn(
            List.of(CvPath.of(dir + "/cv.ttl", dir + "/cv.csv")));

        //when
        harvesterService.harvest(repositoryUrl);

        //then
        IndexOperations vocabularyIndex = elasticsearchOperations.indexOps(of("agid.testvocabulary"));
        vocabularyIndex.refresh();
        assertTrue(vocabularyIndex.exists());

        IndexOperations semanticAssetIndex = elasticsearchOperations.indexOps(of("semantic-asset-metadata"));
        semanticAssetIndex.refresh();
        assertTrue(semanticAssetIndex.exists());

        Response response = given()
                .when()
                .get(String.format(
                        "http://%s/semantic-asset-metadata/_search", elasticsearchContainer.getHttpHostAddress()));

        response.then().statusCode(200)
                .body("hits.hits[0]._source.keyConcept", equalTo("testVocabulary"))
                .body("hits.hits[0]._source.endpointUrl", equalTo("http://localhost:8080/vocabularies/agid/testVocabulary"));
    }
}
