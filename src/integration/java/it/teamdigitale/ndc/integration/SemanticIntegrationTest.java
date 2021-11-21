package it.teamdigitale.ndc.integration;

import static org.testcontainers.utility.DockerImageName.parse;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class SemanticIntegrationTest {

    private static final int VIRTUOSO_PORT = 8890;

    private static GenericContainer virtuoso = new GenericContainer(parse("tenforce/virtuoso"))
            .withReuse(true)
            .withExposedPorts(VIRTUOSO_PORT)
            .withEnv("DBA_PASSWORD", "dba")
            .withEnv("SPARQL_UPDATE", "true");

    @BeforeAll
    static void setup() {
        virtuoso.start();
    }

    @Test
    void shouldExecuteSparqlOnVirtuosoTestcontainer() {
        // given
        String sparqlUrl = "http://localhost:" + virtuoso.getMappedPort(VIRTUOSO_PORT) + "/sparql";
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
        QuerySolution querySolution = resultSet.next();
        assertThat(querySolution).as("Check a valid result row").isNotNull();
        assertThat(querySolution.get("o").toString()).as("Check variable bound value").isEqualTo("This is an example title");
    }

    private void insertSampleData(String sparqlUrl) {
        UpdateRequest updateRequest = new UpdateBuilder()
                .addInsert("<http://example>", "<http://example/egbook>", "<http://example/title>",
                        "This is an example title")
                .buildRequest();
        UpdateExecutionFactory.createRemote(updateRequest, sparqlUrl).execute();
    }
}
