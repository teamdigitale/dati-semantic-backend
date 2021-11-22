package it.teamdigitale.ndc.integration;

import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepositoryProperties;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.DATASET_IRI;
import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.KEY_CONCEPT_IRI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
public class TripleStoreRepositoryTest {

    private static final int VIRTUOSO_PORT = 8890;
    private static final String CV_IRI = "https://w3id.org/italia/controlled-vocabulary/test";
    private static final String RIGHTS_HOLDER_IRI = "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";


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

    private void deleteAllTriplesFromGraph(String graphName, String sparqlUrl) {
        UpdateRequest updateRequest = new UpdateBuilder().addDelete(new SelectBuilder().from(graphName)).buildRequest();
        UpdateExecutionFactory.createRemote(updateRequest, sparqlUrl).execute();
    }
}
