package it.teamdigitale.ndc.integration;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.when;
import static org.testcontainers.utility.DockerImageName.parse;

import io.restassured.response.Response;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class SemanticAssetSearchIntegrationTest {

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
    void shouldIndexRepoAndSearch() throws IOException {
        //given
        String repositoryUrl = "testRepoURL";
        dataIsHarvested(repositoryUrl);

        //when
        harvesterService.harvest(repositoryUrl);

        Response searchResponse =
            when().get(
                String.format("http://localhost:%d/semantic-assets/search?term=ricettive", port));

        //then
        searchResponse.then()
            .statusCode(200)
            .body("totalPages", equalTo(1))
            .body("pageNumber", equalTo(1))
            .body("data.size()", equalTo(1))
            .body("data[0].iri", equalTo(
                "https://w3id.org/italia/controlled-vocabulary/classifications-for-accommodation-facilities/accommodation-star-rating"));

        String iri =
            searchResponse.getBody().as(SemanticAssetSearchResult.class).getData().get(0).getIri();

        Response detailsResponse = when().get(
            String.format(
                "http://localhost:%d/semantic-assets/details?iri=%s", port, iri));

        detailsResponse.then()
            .statusCode(200)
            .body("iri", equalTo(
                "https://w3id.org/italia/controlled-vocabulary/classifications-for-accommodation-facilities/accommodation-star-rating"));
    }

    @Test
    void shouldFailWhenAssetIsNotFoundByIri() throws IOException {
        //given
        String repositoryUrl = "testRepoURL";
        dataIsHarvested(repositoryUrl);

        //when
        harvesterService.harvest(repositoryUrl);

        //then
        Response detailsResponse = when().get(String.format(
            "http://localhost:%d/semantic-assets/details?iri=%s", port, "https://wrong-iri"));

        detailsResponse.then()
            .statusCode(404)
            .body("message", equalTo("Semantic Asset not found for Iri : https://wrong-iri"));

    }

    private String dataIsHarvested(String repositoryUrl) throws IOException {
        String dir = "src/test/resources/testdata";
        Path cloneDir = Path.of(dir);
        when(agencyRepositoryService.cloneRepo(repositoryUrl)).thenReturn(cloneDir);
        when(agencyRepositoryService.getControlledVocabularyPaths(cloneDir)).thenReturn(
            List.of(CvPath.of(dir + "/cv.ttl", dir + "/cv.csv")));
        return repositoryUrl;
    }
}
