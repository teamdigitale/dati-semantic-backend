package it.teamdigitale.ndc.integration;

import static org.testcontainers.utility.DockerImageName.parse;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class SemanticIntegrationTest {

    @Container
    private static GenericContainer virtuoso = new GenericContainer(parse("tenforce/virtuoso"))
        .withReuse(true)
        .withExposedPorts(8890);

    @Test
    void shouldExecuteSparqlOnVirtuosoTestcontainer() {
        String sparqlUrl = "http://localhost:" + virtuoso.getMappedPort(8890) + "/sparql";

        Query selectQuery = new SelectBuilder()
            .addVar("?s").addVar("?p").addVar("?o")
            .addWhere("?s", "?p", "?o")
            .setLimit(10)
            .build();

        ResultSet resultSet =
            QueryExecutionFactory.sparqlService(sparqlUrl, selectQuery)
                .execSelect();

        resultSet.forEachRemaining(result -> {
            System.out.println(result.get("s") + " " + result.get("p") + " " + result.get("o"));
        });
    }
}
