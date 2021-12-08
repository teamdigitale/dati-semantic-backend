package it.teamdigitale.ndc.integration;

import static io.restassured.RestAssured.when;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.SCHEMA;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.restassured.response.Response;
import it.teamdigitale.ndc.controller.dto.SemanticAssetSearchResult;
import it.teamdigitale.ndc.controller.dto.SemanticAssetsSearchDto;
import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class RestApiIntegrationTests {

    private static final ElasticsearchContainer elasticsearchContainer =
        Containers.buildElasticsearchContainer();
    private static final GenericContainer virtuoso = Containers.buildVirtuosoContainer();
    private boolean harvested = false;

    @LocalServerPort
    private int port;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    HarvesterService harvesterService;

    @SpyBean
    AgencyRepositoryService agencyRepositoryService;

    @BeforeAll
    public static void beforeAll() {
        virtuoso.start();
        elasticsearchContainer.start();
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        if (!harvested) {
            dataIsHarvested();
            harvested = true;
        }
    }

    @AfterAll
    public static void tearDown() {
        virtuoso.stop();
        elasticsearchContainer.stop();
    }

    @DynamicPropertySource
    static void updateTestcontainersProperties(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + virtuoso.getMappedPort(Containers.VIRTUOSO_PORT);
        registry.add("virtuoso.sparql.url", () ->  url + "/sparql");
        registry.add("virtuoso.sparql-graph-store.url", () -> url + "/sparql-graph-crud/");
        registry.add("spring.elasticsearch.rest.uris", elasticsearchContainer::getHttpHostAddress);
    }

    @Test
    void shouldBeAbleToHarvestAndSearchControlledVocabularySuccessfully() {
        Response searchResponseForLicenza = getSemanticAsset("Licenza", CONTROLLED_VOCABULARY, 2);

        searchResponseForLicenza.then()
            .statusCode(200)
            .body("totalCount", equalTo(1))
            .body("offset", equalTo(0))
            .body("limit", equalTo(2))
            .body("data.size()", equalTo(1))
            .body("data[0].assetIri",
                equalTo("https://w3id.org/italia/controlled-vocabulary/licences"))
            .body("data[0].rightsHolder.iri",
                equalTo("https://w3id.org/italia/data/public-organization/agid"))
            .body("data[0].rightsHolder.summary", equalTo("Agenzia per l'Italia Digitale"));

        getSemanticAssetDetails(getAssetIri(searchResponseForLicenza)).then()
            .statusCode(200)
            .body("assetIri", equalTo("https://w3id.org/italia/controlled-vocabulary/licences"))
            .body("type", equalTo(CONTROLLED_VOCABULARY.name()))
            .body("keyConcept", equalTo("licences"))
            .body("endpointUrl", equalTo("http://localhost:8080/vocabularies/agid/licences"));
    }

    @Test
    void shouldBeAbleToHarvestAndSearchOntologieSuccessfully() {
        Response searchResponseForRicettivita = getSemanticAsset("Ricettività", ONTOLOGY, 3);

        searchResponseForRicettivita.then()
            .statusCode(200)
            .body("totalCount", equalTo(1))
            .body("offset", equalTo(0))
            .body("limit", equalTo(3))
            .body("data.size()", equalTo(1))
            .body("data[0].assetIri", equalTo("https://w3id.org/italia/onto/ACCO"))
            .body("data[0].rightsHolder.iri",
                equalTo("http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid"))
            .body("data[0].rightsHolder.summary", equalTo("Agenzia per l'Italia Digitale"));

        getSemanticAssetDetails(getAssetIri(searchResponseForRicettivita)).then()
            .statusCode(200)
            .body("assetIri", equalTo("https://w3id.org/italia/onto/ACCO"))
            .body("type", equalTo(ONTOLOGY.name()))
            .body("modifiedOn", equalTo("2018-07-31"))
            .body("keyClasses[0].iri",
                equalTo("https://w3id.org/italia/onto/ACCO/AccommodationRoom"));
    }

    @Test
    void shouldBeAbleToHarvestAndSearchSchemaSuccessfully() {
        Response schemaResponse = getSemanticAsset("The Person schema", SCHEMA, 2);

        schemaResponse.then()
                .statusCode(200)
                .body("totalCount", equalTo(1))
                .body("limit", equalTo(2))
                .body("data.size()", equalTo(1))
                .body("data[0].assetIri",
                        equalTo("https://w3id.org/italia/schema/person/v202108.01/person.oas3.yaml"))
                .body("data[0].rightsHolder.iri",
                        equalTo("http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid"))
                .body("data[0].rightsHolder.summary", equalTo("Agenzia per l'Italia Digitale"));

        getSemanticAssetDetails(getAssetIri(schemaResponse)).then()
                .statusCode(200)
                .body("assetIri",
                        equalTo("https://w3id.org/italia/schema/person/v202108.01/person.oas3.yaml"))
                .body("type", equalTo(SCHEMA.name()))
                .body("modifiedOn", equalTo("2021-12-06"))
                .body("distributionUrls[0]",
                        equalTo("https://github.com/ioggstream/json-semantic-playground/tree/master/assets/schemas/person/v202108.01"));
    }

    @Test
    void shouldBeAbleToHarvestOnlyLatestFolderOfSemanticAssetAndSearchSuccessfully() {
        //alberghiere is the keyword in testdata/Ontologie/ACCO/v0.4/ACCO-AP_IT.ttl
        getSemanticAsset("alberghiere", ONTOLOGY, 2).then()
                .statusCode(200)
                .body("totalCount", equalTo(0))
                .body("offset", equalTo(0))
                .body("limit", equalTo(2))
                .body("data.size()", equalTo(0));

        getSemanticAsset("Ricettività", ONTOLOGY, 3).then()
                .statusCode(200)
                .body("totalCount", equalTo(1))
                .body("data.size()", equalTo(1))
                .body("data[0].assetIri", equalTo("https://w3id.org/italia/onto/ACCO"));
    }

    @Test
    void shouldBeAbleToHarvestLatestVersionOfSemanticAssetAndSearchSuccessfully() {
        //vecchia versione is the keyword in testdata/Ontologie/CLV/0.8/CLV-AP_IT.ttl
        getSemanticAsset("vecchia versione", ONTOLOGY, 3).then()
                .statusCode(200)
                .body("totalCount", equalTo(0))
                .body("offset", equalTo(0))
                .body("limit", equalTo(3))
                .body("data.size()", equalTo(0));

        getSemanticAsset("Indirizzo", ONTOLOGY, 3).then()
                .statusCode(200)
                .body("totalCount", equalTo(1))
                .body("data.size()", equalTo(1))
                .body("data[0].assetIri", equalTo("https://w3id.org/italia/onto/CLV"));
    }

    @Test
    void shouldBeAbleToFilterSemanticAssetByTypeSuccessfully() {
        List<SemanticAssetsSearchDto> semanticAssets = getSemanticAsset("", ONTOLOGY, 5)
                .then()
                .statusCode(200)
                .extract()
                .as(SemanticAssetSearchResult.class)
                .getData();

        assertTrue(semanticAssets.stream().allMatch(semanticAssetsSearchDto ->
                semanticAssetsSearchDto.getType().equals(ONTOLOGY)));
    }

    @Test
    void shouldBeAbleToRetrieveSemanticAssetByOffsetSuccessfully() {
        List<SemanticAssetsSearchDto> semanticAssetsSearch = when()
                .get(format("http://localhost:%d/semantic-assets?q=%s&limit=%s",
                        port, "", 2))
                .then()
                .statusCode(200)
                .extract()
                .as(SemanticAssetSearchResult.class)
                .getData();
        semanticAssetsSearch.remove(0);

        SemanticAssetSearchResult semanticAssetSearchResult = when()
                .get(format("http://localhost:%d/semantic-assets?q=%s&limit=%s&offset=%s",
                        port, "", 1, 1))
                .then()
                .statusCode(200)
                .extract()
                .as(SemanticAssetSearchResult.class);

        assertEquals(1, semanticAssetSearchResult.getOffset());
        assertEquals(1, semanticAssetSearchResult.getLimit());
        assertEquals(semanticAssetsSearch, semanticAssetSearchResult.getData());
    }

    @Test
    void shouldBeAbleToHarvestAndSearchSemanticAssetWhenFlatFileIsMissing() {
        Response searchResponse = getSemanticAsset("Istruzione", CONTROLLED_VOCABULARY, 4);

        searchResponse.then()
            .statusCode(200)
            .body("totalCount", equalTo(1))
            .body("offset", equalTo(0))
            .body("limit", equalTo(4))
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
            .body("type", equalTo(CONTROLLED_VOCABULARY.name()))
            .body("keyConcept", equalTo("educationTitle"))
            .body("endpointUrl", equalTo(""));
    }

    @Test
    void shouldNotHarvestCorruptedControlledVocabulary() {
        Response searchResponse = getSemanticAsset("Appellativo", CONTROLLED_VOCABULARY, 5);

        searchResponse.then()
            .statusCode(200)
            .body("totalCount", equalTo(0))
            .body("offset", equalTo(0))
            .body("limit", equalTo(5))
            .body("data.size()", equalTo(0));

        when().get(String.format("http://localhost:%d/agid/personTitle", port)).then()
            .statusCode(404);
    }

    @Test
    void shouldNotHarvestControlledVocabularyIfKeyConceptIsMissing() {
        Response searchResponse = getSemanticAsset("scientifiche", CONTROLLED_VOCABULARY, 6);

        searchResponse.then()
            .statusCode(200)
            .body("totalCount", equalTo(0))
            .body("offset", equalTo(0))
            .body("limit", equalTo(6))
            .body("data.size()", equalTo(0));
    }

    @Test
    void shouldFailWhenAssetIsNotFoundByIri() {
        Response detailsResponse = getSemanticAssetDetails("https://wrong-iri");

        detailsResponse.then()
            .statusCode(404)
            .body("message", equalTo("Semantic Asset not found for Iri : https://wrong-iri"));
    }

    private void dataIsHarvested() throws IOException {
        String dir = "src/test/resources/testdata";
        Path cloneDir = Path.of(dir);
        doReturn(cloneDir).when(agencyRepositoryService).cloneRepo("http://testRepoURL");
        doNothing().when(agencyRepositoryService).removeClonedRepo(cloneDir);

        harvesterService.harvest("http://testRepoURL");

        elasticsearchOperations.indexOps(SemanticAssetMetadata.class).refresh();
    }

    private Response getSemanticAsset(String searchTerm, SemanticAssetType semanticAssetType,
                                      int limit) {
        return when().get(format("http://localhost:%d/semantic-assets?q=%s&type=%s&limit=%s", port,
            searchTerm, semanticAssetType.name(), limit));
    }

    private Response getSemanticAssetDetails(String iri) {
        return when().get(format("http://localhost:%d/semantic-assets/byIri?iri=%s", port, iri));
    }

    private String getAssetIri(Response searchResponse) {
        return searchResponse.getBody().as(SemanticAssetSearchResult.class).getData().get(0)
            .getAssetIri();
    }
}
