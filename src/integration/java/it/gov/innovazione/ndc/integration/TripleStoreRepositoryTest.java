package it.gov.innovazione.ndc.integration;

import it.gov.innovazione.ndc.repository.TripleStoreProperties;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import it.gov.innovazione.ndc.repository.VirtuosoClient;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class TripleStoreRepositoryTest {

    private static String sparqlUrl;
    private static String sparqlGraphUrl;
    private static final String graphName = "http://www.fantasy.org/graph";
    private static final String oldGraphName = "http://tmp.www.fantasy.org/graph";

    private static TripleStoreRepository repository;

    private static final GenericContainer virtuoso = Containers.buildVirtuosoContainer();

    @BeforeAll
    public static void beforeAll() {
        virtuoso.start();
        String baseUrl = "http://localhost:" + virtuoso.getMappedPort(Containers.VIRTUOSO_PORT);
        sparqlUrl = baseUrl + "/sparql";
        sparqlGraphUrl = baseUrl + "/sparql-graph-crud";
        TripleStoreProperties properties =
            TripleStoreProperties.builder()
                .sparql(sparqlUrl)
                .sparqlGraphStore(sparqlGraphUrl)
                .username("")
                .password("")
                .build();

        repository = new TripleStoreRepository(new VirtuosoClient(properties));
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
                .from(oldGraphName)
            .build();

        try (RDFConnection conn = RDFConnectionRemote.create()
                .destination(sparqlUrl)
                .build();
             QueryExecution qExec = conn.query(findPeriodicity)) {

            ResultSet resultSet = qExec.execSelect();
            assertThat(resultSet.hasNext()).isTrue();

            QuerySolution querySolution = resultSet.next();
            assertThat(querySolution).as("Check a valid result row").isNotNull();
            assertThat(querySolution.get("o").toString())
                    .as("Check variable bound value")
                    .isEqualTo("http://publications.europa.eu/resource/authority/frequency/IRREG");
        }
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
                .from(oldGraphName)
            .build();

        try (RDFConnection conn = RDFConnectionRemote.create()
                .destination(sparqlUrl)
                .build();
             QueryExecution qExec = conn.query(keywordQuery)) {

            ResultSet resultSet = qExec.execSelect();
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
        QueryExecution queryExecution = repository.select(findTitle);
        ResultSet resultSet = queryExecution.execSelect();
        assertThat(resultSet.hasNext()).isTrue();
        assertThat(resultSet.next().get("b").asResource().getURI()).isEqualTo(
            "http://example/egbook");
        assertThat(resultSet.hasNext()).isFalse();
        queryExecution.close();

        // when
        repository.clearExistingNamedGraph("http://agid");
        QueryExecution queryExecution1 = repository.select(findTitle);
        resultSet = queryExecution1.execSelect();

        // then
        assertThat(resultSet.hasNext()).isFalse();
        queryExecution1.close();
    }

    private void deleteAllTriplesFromGraph(String graphName, String sparqlUrl, String sparqlGraphUrl) {
        try (RDFConnection conn = RDFConnectionRemote.create()
                .queryEndpoint(sparqlUrl)   // SPARQL query (e update, se è lo stesso)
                .updateEndpoint(sparqlUrl)  // SPARQL update (spesso è lo stesso endpoint di query)
                .gspEndpoint(sparqlGraphUrl) // Graph Store Protocol endpoint
                .build()) {

            boolean graphExists = conn.queryAsk("ASK WHERE { GRAPH <" + graphName + "> { ?s ?p ?o } }");
            if (graphExists) {
                conn.delete(graphName); // cancella l'intero named graph via GSP
            }
        }
    }

}
