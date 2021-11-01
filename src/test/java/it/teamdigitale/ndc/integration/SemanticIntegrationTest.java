package it.teamdigitale.ndc.integration;

import static org.testcontainers.utility.DockerImageName.parse;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class SemanticIntegrationTest {

    private static GenericContainer virtuoso = new GenericContainer(parse("tenforce/virtuoso"))
        .withReuse(true)
        .withExposedPorts(8890)
        .withEnv("DBA_PASSWORD", "dba")
        .withEnv("SPARQL_UPDATE", "true");

    @BeforeAll
    static void setup() {
        virtuoso.start();
    }

    @Test
    void shouldExecuteSparqlOnVirtuosoTestcontainer() {
        // given
        String sparqlUrl = "http://localhost:" + virtuoso.getMappedPort(8890) + "/sparql";
        insertSampleData(sparqlUrl);

        // when
        Query findTitle = new SelectBuilder()
            .addVar("o")
            .addWhere("<http://example/egbook>", "<http://example/title>", "?o")
            .from("http://example")
            .build();
        ResultSet resultSet =
            QueryExecutionFactory.sparqlService(sparqlUrl, findTitle).execSelect();

        // then
        assert resultSet.next().get("o").toString().equals("This is an example title");
    }

    private void insertSampleData(String sparqlUrl) {
        UpdateRequest updateRequest = new UpdateBuilder()
            .addInsert("<http://example>", "<http://example/egbook>", "<http://example/title>",
                "This is an example title")
            .buildRequest();
        UpdateExecutionFactory.createRemote(updateRequest, sparqlUrl).execute();
    }
}
