package it.teamdigitale.ndc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepositoryProperties;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TripleStoreRepositoryTest {

    private static final int VIRTUOSO_PORT = 8890;

    @Container
    private static GenericContainer virtuoso = new GenericContainer(parse("tenforce/virtuoso"))
            .withReuse(true)
            .withExposedPorts(VIRTUOSO_PORT)
            .withEnv("DBA_PASSWORD", "dba")
            .withEnv("SPARQL_UPDATE", "true");

    @Test
    void shouldExecuteSparqlOnVirtuosoTestcontainer() {
        final String graphName = this.getClass().getSimpleName();

        // given
        String sparqlUrl = "http://localhost:" + virtuoso.getMappedPort(VIRTUOSO_PORT) + "/sparql";
        TripleStoreRepositoryProperties properties = new TripleStoreRepositoryProperties(sparqlUrl, "dba", "dba");
        Model model = RDFDataMgr.loadModel("src/test/resources/testdata/onto.ttl");
        deleteAllTriplesFromGraph(graphName, sparqlUrl);

        // when
        TripleStoreRepository repository = new TripleStoreRepository(properties);
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
    void shouldDeleteOnlyTheSpecifiedNamedGraph() {
        final String repoUrl = "http://agid";

        // given
        String sparqlUrl = "http://localhost:" + virtuoso.getMappedPort(VIRTUOSO_PORT) + "/sparql";
        TripleStoreRepositoryProperties properties = new TripleStoreRepositoryProperties(sparqlUrl, "dba", "dba");
        TripleStoreRepository repository = new TripleStoreRepository(properties);
        UpdateRequest updateRequest = new UpdateBuilder()
                .addInsert("<http://agid>", "<http://example/egbook>", "<http://example/title>",
                        "This is an example title")
                .addInsert("<http://istat>", "<http://example/anotherBook>", "<http://example/title>",
                        "Something different")
                .buildRequest();
        UpdateExecutionFactory.createRemote(updateRequest, sparqlUrl).execute();

        // when
        repository.clearExistingNamedGraph(repoUrl);

        // then
        Query findTitle = new SelectBuilder()
                .addVar("b")
                .addVar("t")
                .addWhere(
                        "?b",
                        "<http://example/title>",
                        "?t"
                )
                .build();
        ResultSet resultSet =
                QueryExecutionFactory.sparqlService(sparqlUrl, findTitle).execSelect();

        assertThat(resultSet.hasNext()).isTrue();
        QuerySolution querySolution = resultSet.next();
        assertThat(querySolution.get("b").toString()).as("Check variable bound value")
                .isEqualTo("http://example/anotherBook");
        assertThat(resultSet.hasNext()).isFalse();
    }

    private void deleteAllTriplesFromGraph(String graphName, String sparqlUrl) {
        UpdateRequest updateRequest = new UpdateBuilder().addDelete(new SelectBuilder().from(graphName)).buildRequest();
        UpdateExecutionFactory.createRemote(updateRequest, sparqlUrl).execute();
    }
}
