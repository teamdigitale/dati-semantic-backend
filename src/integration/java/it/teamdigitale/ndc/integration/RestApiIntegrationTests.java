package it.teamdigitale.ndc.integration;

import io.restassured.response.Response;
import it.teamdigitale.ndc.gen.dto.AssetType;
import it.teamdigitale.ndc.gen.dto.SearchResult;
import it.teamdigitale.ndc.gen.dto.SearchResultItem;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.model.profiles.NDC;
import junit.framework.AssertionFailedError;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static io.restassured.RestAssured.when;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.teamdigitale.ndc.harvester.SemanticAssetType.SCHEMA;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestApiIntegrationTests extends BaseIntegrationTest {
    @DynamicPropertySource
    static void updateDynamicPropertySource(DynamicPropertyRegistry registry) {
        updateTestcontainersProperties(registry);
    }


    @Test
    void shouldBeAbleToHarvestAndSearchControlledVocabularySuccessfully() {
        final String assetIri = "https://w3id.org/italia/controlled-vocabulary/licences";
        final String rightsHolderName = "Agenzia per l'Italia Digitale";
        final String rightsHolderIri = "https://w3id.org/italia/data/public-organization/agid";
        final String rightsHolder = "http://purl.org/dc/terms/rightsHolder";

        Response searchResponseForLicenza = getSemanticAsset("Licenza", CONTROLLED_VOCABULARY, 2);

        searchResponseForLicenza.then()
                .statusCode(200)
                .body("totalCount", equalTo(1))
                .body("offset", equalTo(0))
                .body("limit", equalTo(2))
                .body("data.size()", equalTo(1))
                .body("data[0].assetIri", equalTo(assetIri))
                .body("data[0].rightsHolder.iri", equalTo(rightsHolderIri))
                .body("data[0].rightsHolder.summary", equalTo(rightsHolderName));

        getSemanticAssetDetails(getAssetIri(searchResponseForLicenza)).then()
                .statusCode(200)
                .body("assetIri", equalTo(assetIri))
                .body("type", equalTo(CONTROLLED_VOCABULARY.name()))
                .body("keyConcept", equalTo("licences"))
                .body("endpointUrl", containsString("vocabularies/agid/licences"));

        try (RDFConnection connection = getVirtuosoConnection()) {
            String query = format("SELECT ?o WHERE { <%s> <%s> ?o }", assetIri, rightsHolder);
            ResultSet resultSet = connection.query(query).execSelect();
            assertThat(resultSet.hasNext()).isTrue();

            QuerySolution querySolution = resultSet.next();
            assertThat(querySolution).isNotNull();
            assertThat(querySolution.get("o").toString()).isEqualTo(rightsHolderIri);
        }
    }

    @Test
    void shouldBeAbleToHarvestAndSearchOntologieSuccessfully() {
        final String assetIri = "https://w3id.org/italia/onto/ACCO";
        final String rightsHolderIri = "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
        final String title = "http://purl.org/dc/terms/title";

        Response searchResponseForRicettivita = getSemanticAsset("Ricettività", ONTOLOGY, 3);

        searchResponseForRicettivita.then()
                .statusCode(200)
                .body("totalCount", equalTo(1))
                .body("offset", equalTo(0))
                .body("limit", equalTo(3))
                .body("data.size()", equalTo(1))
                .body("data[0].assetIri", equalTo(assetIri))
                .body("data[0].rightsHolder.iri", equalTo(rightsHolderIri))
                .body("data[0].rightsHolder.summary", equalTo("Agenzia per l'Italia Digitale"));

        getSemanticAssetDetails(getAssetIri(searchResponseForRicettivita)).then()
                .statusCode(200)
                .body("assetIri", equalTo(assetIri))
                .body("type", equalTo(ONTOLOGY.name()))
                .body("modifiedOn", equalTo("2018-07-31"))
                .body("keyClasses[0].iri",
                        equalTo("https://w3id.org/italia/onto/ACCO/AccommodationRoom"));

        try (RDFConnection connection = getVirtuosoConnection()) {
            String query = format("SELECT ?o WHERE { <%s> <%s> ?o }", assetIri, title);
            ResultSet resultSet = connection.query(query).execSelect();
            assertThat(resultSet.hasNext()).isTrue();

            QuerySolution querySolution = resultSet.next();
            assertThat(querySolution).isNotNull();
            assertThat(querySolution.get("o").toString())
                    .isEqualTo("Accommodation Facilities Ontology - Italian Application Profile@en");
        }
    }

    @Test
    void shouldBeAbleToHarvestAndSearchSchemaSuccessfully() {
        final String assetIri = "https://w3id.org/italia/schema/person/v202108.01/person.oas3.yaml";
        final String rightsHolderIri = "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
        final String rightsHolderName = "Agenzia per l'Italia Digitale";
        final String title = "http://purl.org/dc/terms/title";

        Response schemaResponse = getSemanticAsset("The Person schema", SCHEMA, 2);

        schemaResponse.then()
                .statusCode(200)
                .body("totalCount", equalTo(1))
                .body("limit", equalTo(2))
                .body("data.size()", equalTo(1))
                .body("data[0].assetIri", equalTo(assetIri))
                .body("data[0].rightsHolder.iri", equalTo(rightsHolderIri))
                .body("data[0].rightsHolder.summary", equalTo(rightsHolderName));

        getSemanticAssetDetails(getAssetIri(schemaResponse)).then()
                .statusCode(200)
                .body("assetIri", equalTo(assetIri))
                .body("type", equalTo(SCHEMA.name()))
                .body("modifiedOn", equalTo("2021-12-06"))
                .body("distributionUrls[0]",
                        equalTo("https://github.com/ioggstream/json-semantic-playground/tree/master/assets/schemas/person/v202108.01"));

        try (RDFConnection connection = getVirtuosoConnection()) {
            String query = format("SELECT ?o WHERE { <%s> <%s> ?o }", assetIri, title);
            ResultSet resultSet = connection.query(query).execSelect();
            assertThat(resultSet.hasNext()).isTrue();

            QuerySolution querySolution = resultSet.next();
            assertThat(querySolution).isNotNull();
            assertThat(querySolution.get("o").toString()).isEqualTo("The Person schema");
        }
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
        List<SearchResultItem> semanticAssets = getSemanticAsset("", ONTOLOGY, 5)
                .then()
                .statusCode(200)
                .extract()
                .as(SearchResult.class)
                .getData();

        assertTrue(semanticAssets.stream().allMatch(dto -> dto.getType().equals(AssetType.ONTOLOGY)));
    }

    @Test
    void shouldBeAbleToRetrieveSemanticAssetByOffsetSuccessfully() {
        List<SearchResultItem> semanticAssetsSearch = when()
                .get(format("http://localhost:%d/semantic-assets?q=%s&limit=%s",
                        port, "", 2))
                .then()
                .statusCode(200)
                .extract()
                .as(SearchResult.class)
                .getData();
        semanticAssetsSearch.remove(0);

        SearchResult semanticAssetSearchResult = when()
                .get(format("http://localhost:%d/semantic-assets?q=%s&limit=%s&offset=%s",
                        port, "", 1, 1))
                .then()
                .statusCode(200)
                .extract()
                .as(SearchResult.class);

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
        String fakeUri = "https://wrong-iri";
        Response detailsResponse = getSemanticAssetDetails(fakeUri);

        detailsResponse.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("title", containsString(fakeUri));
    }

    @Test
    void shouldRetrieveDataServiceFromQueryingSparql() {
        try (RDFConnection connection = getVirtuosoConnection()) {
            String enrichedDataset = "https://w3id.org/italia/controlled-vocabulary/licences";
            String keyConcept = "licences";
            String agencyId = "agid";
            String expectedDataService = "https://w3id.org/italia/data/data-service/agid-licences";
            String expectedEndpointUrl = String.format("https://ndc-dev.apps.cloudpub.testedev.istat.it/api/vocabularies/%s/%s", agencyId, keyConcept);

            String query = format("SELECT ?ds ?du WHERE { ?ds <%s> <%s> ; <%s> ?du }",
                    NDC.servesDataset.getURI(), enrichedDataset, NDC.endpointURL.getURI());
            ResultSet resultSet = connection.query(query).execSelect();

            assertThat(resultSet.hasNext()).isTrue();
            QuerySolution querySolution = resultSet.next();

            RDFNode dataService = querySolution.get("ds");
            assertThat(dataService).isNotNull();
            assertThat(dataService.toString()).isEqualTo(expectedDataService);

            RDFNode downloadUrl = querySolution.get("du");
            assertThat(downloadUrl).isNotNull();
            assertThat(downloadUrl.toString()).isEqualTo(expectedEndpointUrl);
        }
    }

    private RDFConnection getVirtuosoConnection() {
        String sparql = virtuosoProps.getSparql();
        String graphProtocolUrl = virtuosoProps.getSparqlGraphStore();
        RDFConnection connection = RDFConnectionFactory.connect(sparql, sparql, graphProtocolUrl);
        if (connection == null) {
            throw new AssertionFailedError("Could not connect to Virtuoso");
        }
        return connection;
    }

    private Response getSemanticAsset(String searchTerm, SemanticAssetType semanticAssetType,
                                      int limit) {
        return when().get(format("http://localhost:%d/semantic-assets?q=%s&type=%s&limit=%s", port,
                searchTerm, semanticAssetType.name(), limit));
    }

    private Response getSemanticAssetDetails(String iri) {
        return when().get(format("http://localhost:%d/semantic-assets/by-iri?iri=%s", port, iri));
    }

    private String getAssetIri(Response searchResponse) {
        return searchResponse.getBody().as(SearchResult.class).getData().get(0)
                .getAssetIri();
    }
}
