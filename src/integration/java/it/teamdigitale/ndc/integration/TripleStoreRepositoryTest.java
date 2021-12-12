package it.teamdigitale.ndc.integration;

import static org.assertj.core.api.Assertions.assertThat;

import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepositoryProperties;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TripleStoreRepositoryTest {

    private static String sparqlUrl;
    private static String sparqlGraphUrl;
    private static final String graphName = "http://www.fantasy.org/graph";

    private static TripleStoreRepository repository;

    private static final GenericContainer virtuoso = Containers.buildVirtuosoContainer();

    @BeforeAll
    public static void beforeAll() {
        virtuoso.start();
        String baseUrl = "http://localhost:" + virtuoso.getMappedPort(Containers.VIRTUOSO_PORT);
        sparqlUrl = baseUrl + "/sparql";
        sparqlGraphUrl = baseUrl + "/sparql-graph-crud/";
        repository = new TripleStoreRepository(TripleStoreRepositoryProperties.forAnonymousBaseUrl(
            baseUrl));
    }

    @BeforeEach
    public void beforeEach() {
        deleteAllTriplesFromGraph(graphName, sparqlUrl, sparqlGraphUrl);
        deleteAllTriplesFromGraph("http://agid", sparqlUrl, sparqlGraphUrl);
        deleteAllTriplesFromGraph("http://istat", sparqlUrl, sparqlGraphUrl);
    }

    @Test
    void shouldSaveGivenModelInVirtuosoTestcontainer() {
        // given
        Model model = RDFDataMgr.loadModel("src/test/resources/testdata/onto.ttl");

        // when
        repository.save(graphName, model);

        // then
        Query findPeriodicity = new SelectBuilder()
            .addVar("o")
            .addWhere(
                "<https://w3id.org/italia/onto/CulturalHeritage>",
                "<http://purl.org/dc/terms/accrualPeriodicity>",
                "?o"
            )
            .from(graphName)
            .build();
        ResultSet resultSet =
            QueryExecutionFactory.sparqlService(sparqlUrl, findPeriodicity).execSelect();

        assertThat(resultSet.hasNext()).isTrue();
        QuerySolution querySolution = resultSet.next();
        assertThat(querySolution).as("Check a valid result row").isNotNull();
        assertThat(querySolution.get("o").toString()).as("Check variable bound value")
            .isEqualTo("http://publications.europa.eu/resource/authority/frequency/IRREG");
    }

    @Test
    void shouldSaveGivenModelWithBlankNodeInVirtuosoTestcontainer() {
        // given
        Model model = RDFDataMgr.loadModel("src/test/resources/testdata/onto-with-blank.ttl");

        // when
        repository.save(graphName, model);

        // then
        Query keywordQuery = new SelectBuilder()
            .addVar("k")
            .addWhere(
                "<https://w3id.org/italia/onto/CulturalHeritage>",
                "<http://www.w3.org/ns/dcat#keyword>",
                "?k"
            )
            .from(graphName)
            .build();
        ResultSet resultSet =
            QueryExecutionFactory.sparqlService(sparqlUrl, keywordQuery).execSelect();

        assertThat(resultSet.hasNext()).isTrue();

        QuerySolution querySolution = resultSet.next();
        assertThat(querySolution).as("Check a valid result row").isNotNull();

        RDFNode keywordNode = querySolution.get("k");
        assertThat(keywordNode).isNotNull();

        assertThat(keywordNode.isLiteral()).isTrue();
        Literal keyword = (Literal) keywordNode;
        assertThat(keyword.getLanguage()).isEqualTo("it");
        assertThat(keyword.getString()).isEqualTo("Beni culturali");
    }

    @Test
    void shouldDeleteOnlyTheSpecifiedNamedGraph() {
        // given
        SelectBuilder findTitle = new SelectBuilder()
            .addVar("b")
            .addVar("t")
            .addWhere(
                "?b",
                "<http://example/title>",
                "?t"
            );
        UpdateRequest updateRequest = new UpdateBuilder()
            .addInsert("<http://agid>", "<http://example/egbook>", "<http://example/title>",
                "This is an example title")
            .buildRequest();
        UpdateExecutionFactory.createRemote(updateRequest, sparqlUrl).execute();

        //when
        ResultSet resultSet = repository.select(findTitle).execSelect();
        assertThat(resultSet.hasNext()).isTrue();
        assertThat(resultSet.next().get("b").asResource().getURI()).isEqualTo(
            "http://example/egbook");
        assertThat(resultSet.hasNext()).isFalse();

        // when
        repository.clearExistingNamedGraph("http://agid");
        resultSet = repository.select(findTitle).execSelect();

        // then
        assertThat(resultSet.hasNext()).isFalse();
    }

    private void deleteAllTriplesFromGraph(String graphName, String sparqlUrl,
                                           String sparqlGraphlUrl) {
        RDFConnection connect =
            RDFConnectionFactory.connect(sparqlUrl, sparqlGraphlUrl, sparqlGraphlUrl);
        boolean graphExists =
            connect.queryAsk("ASK WHERE { GRAPH <" + graphName + "> { ?s ?p ?o } }");
        if (graphExists) {
            connect.delete(graphName);
        }
    }

}
