package it.teamdigitale.ndc.integration;

import static io.restassured.RestAssured.when;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.restassured.response.Response;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class RestApiIntegrationTests {

    private static final ElasticsearchContainer elasticsearchContainer =
        Containers.buildElasticsearchContainer();

    @LocalServerPort
    private int port;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    HarvesterService harvesterService;

    @SpyBean
    AgencyRepositoryService agencyRepositoryService;

    @MockBean
    TripleStoreRepository tripleStoreRepository;

    @BeforeAll
    public static void beforeAll() {
        elasticsearchContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        elasticsearchContainer.stop();
    }

    @DynamicPropertySource
    static void updateTestcontainersProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.rest.uris", elasticsearchContainer::getHttpHostAddress);
    }

    @Test
    void shouldBeAbleToHarvestAndSearchControlledVocabularySuccessfully() throws IOException {
        //given
        dataIsHarvested();

        //when
        Response searchResponseForLicenza =
            getSemanticAsset("Licenza", SemanticAssetType.CONTROLLED_VOCABULARY);

        //then
        searchResponseForLicenza.then()
            .statusCode(200)
            .body("totalCount", equalTo(1))
            .body("offset", equalTo(0))
            .body("limit", equalTo(10))
            .body("data.size()", equalTo(1))
            .body("data[0].assetIri",
                equalTo("https://w3id.org/italia/controlled-vocabulary/licences"))
            .body("data[0].rightsHolder.iri",
                equalTo("https://w3id.org/italia/data/public-organization/agid"))
            .body("data[0].rightsHolder.summary", equalTo("Agenzia per l'Italia Digitale"));

        getSemanticAssetDetails(getAssetIri(searchResponseForLicenza)).then()
            .statusCode(200)
            .body("assetIri", equalTo("https://w3id.org/italia/controlled-vocabulary/licences"))
            .body("type", equalTo(SemanticAssetType.CONTROLLED_VOCABULARY.name()))
            .body("keyConcept", equalTo("licences"))
            .body("endpointUrl", equalTo("http://localhost:8080/vocabularies/agid/licences"));
    }

    @Test
    void shouldBeAbleToHarvestAndSearchOntologieSuccessfully() throws IOException {
        //given
        dataIsHarvested();

        //when
        Response searchResponseForRicettivita =
            getSemanticAsset("Ricettività", SemanticAssetType.ONTOLOGY);

        //then
        searchResponseForRicettivita.then()
            .statusCode(200)
            .body("totalCount", equalTo(1))
            .body("offset", equalTo(0))
            .body("limit", equalTo(10))
            .body("data.size()", equalTo(1))
            .body("data[0].assetIri", equalTo("https://w3id.org/italia/onto/ACCO"))
            .body("data[0].rightsHolder.iri",
                equalTo("http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid"))
            .body("data[0].rightsHolder.summary", equalTo("Agenzia per l'Italia Digitale"));

        getSemanticAssetDetails(getAssetIri(searchResponseForRicettivita)).then()
            .statusCode(200)
            .body("assetIri", equalTo("https://w3id.org/italia/onto/ACCO"))
            .body("type", equalTo(SemanticAssetType.ONTOLOGY.name()))
            .body("modifiedOn", equalTo("2018-07-31"))
            .body("keyClasses[0].iri",
                equalTo("https://w3id.org/italia/onto/ACCO/AccommodationRoom"));
    }

    @Test
    void shouldBeAbleToHarvestAndSearchSemanticAssetWhenFlatFileIsMissing() throws IOException {
        //given
        dataIsHarvested();

        //when
        Response searchResponse =
            getSemanticAsset("Istruzione", SemanticAssetType.CONTROLLED_VOCABULARY);

        //then
        searchResponse.then()
            .statusCode(200)
            .body("totalCount", equalTo(1))
            .body("offset", equalTo(0))
            .body("limit", equalTo(10))
            .body("data.size()", equalTo(1))
            .body("data[0].assetIri", equalTo(
                "https://w3id.org/italia/controlled-vocabulary/classifications-for-people/education-level"))
            .body("data[0].rightsHolder.iri", equalTo(
                "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/ISTAT"))
            .body("data[0].rightsHolder.summary",
                equalTo("Istituto Nazionale di Statistica - ISTAT"));

        getSemanticAssetDetails(getAssetIri(searchResponse)).then()
            .statusCode(200)
            .body("assetIri", equalTo(
                "https://w3id.org/italia/controlled-vocabulary/classifications-for-people/education-level"))
            .body("type", equalTo(SemanticAssetType.CONTROLLED_VOCABULARY.name()))
            .body("keyConcept", equalTo("educationTitle"))
            .body("endpointUrl", equalTo(""));
    }

    @Test
    void shouldNotHarvestCorruptedControlledVocabulary() throws IOException {
        //given
        dataIsHarvested();

        //when
        Response searchResponse =
            getSemanticAsset("Appellativo", SemanticAssetType.CONTROLLED_VOCABULARY);

        //then
        searchResponse.then()
            .statusCode(200)
            .body("totalCount", equalTo(0))
            .body("offset", equalTo(0))
            .body("limit", equalTo(10))
            .body("data.size()", equalTo(0));

        when().get(String.format("http://localhost:%d/agid/personTitle", port)).then()
            .statusCode(404);
    }

    @Test
    void shouldNotHarvestControlledVocabularyIfKeyConceptIsMissing() throws IOException {
        //given
        dataIsHarvested();

        //when
        Response searchResponse =
            getSemanticAsset("scientifiche", SemanticAssetType.CONTROLLED_VOCABULARY);

        //then
        searchResponse.then()
            .statusCode(200)
            .body("totalCount", equalTo(0))
            .body("offset", equalTo(0))
            .body("limit", equalTo(10))
            .body("data.size()", equalTo(0));
    }

    @Test
    void shouldFailWhenAssetIsNotFoundByIri() throws IOException {
        //when
        dataIsHarvested();

        //then
        Response detailsResponse = getSemanticAssetDetails("https://wrong-iri");

        detailsResponse.then()
            .statusCode(404)
            .body("message", equalTo("Semantic Asset not found for Iri : https://wrong-iri"));
    }

    private void dataIsHarvested() throws IOException {
        String dir = "src/test/resources/testdata";
        Path cloneDir = Path.of(dir);
        doReturn(cloneDir).when(agencyRepositoryService).cloneRepo("testRepoURL");
        doNothing().when(agencyRepositoryService).removeClonedRepo(cloneDir);

        harvesterService.harvest("testRepoURL");

        elasticsearchOperations.indexOps(SemanticAssetMetadata.class).refresh();
    }

    private Response getSemanticAsset(String searchTerm, SemanticAssetType semanticAssetType) {
        return when().get(format("http://localhost:%d/semantic-assets?q=%s&type=%s", port,
            searchTerm, semanticAssetType.name()));
    }

    private Response getSemanticAssetDetails(String iri) {
        return when().get(format("http://localhost:%d/semantic-assets/byIri?iri=%s", port, iri));
    }

    private String getAssetIri(Response searchResponse) {
        return searchResponse.getBody().as(SemanticAssetSearchResult.class).getData().get(0)
            .getAssetIri();
    }
}
