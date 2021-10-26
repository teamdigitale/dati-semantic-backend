package it.teamdigitale.ndc.integration;

import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.tp;
import static org.testcontainers.utility.DockerImageName.parse;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLTupleQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
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

        SPARQLRepository sparqlRepository = new SPARQLRepository(sparqlUrl);

        String selectAll = Queries.SELECT()
            .where(tp(var("s"), var("p"), var("o")))
            .getQueryString();

        TupleQueryResult result = ((SPARQLTupleQuery) sparqlRepository.getConnection()
            .prepareQuery(selectAll)).evaluate();

        result.stream().forEach(statement ->
            System.out.println(statement.getValue("s"))
        );
    }
}
