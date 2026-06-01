package it.gov.innovazione.ndc.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OntologyDeltaClassifierTest {

    private static final String ASSET = "https://w3id.org/example/onto/foo";
    private static final String CLASS_A = ASSET + "/ClasseA";
    private static final String CLASS_B = ASSET + "/ClasseB";
    private static final String PROP_X = ASSET + "/propX";

    private final OntologyDeltaClassifier classifier = new OntologyDeltaClassifier();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void supportsOnlyOntology() {
        assertThat(classifier.supports(SemanticAssetType.ONTOLOGY)).isTrue();
        assertThat(classifier.supports(SemanticAssetType.CONTROLLED_VOCABULARY)).isFalse();
        assertThat(classifier.supports(SemanticAssetType.SCHEMA)).isFalse();
    }

    @Test
    void emptyDiffReturnsEmpty() {
        Model empty = ModelFactory.createDefaultModel();
        assertThat(classifier.classify(ASSET, empty, empty, empty, empty)).isEmpty();
    }

    @Test
    void addedScenarioReportsCountsAndEmptyLists() throws Exception {
        Model tmp = ModelFactory.createDefaultModel();
        addClass(tmp, CLASS_A, "Classe A");
        addClass(tmp, CLASS_B, "Classe B");
        addProperty(tmp, PROP_X);
        Model empty = ModelFactory.createDefaultModel();

        Optional<String> result = classifier.classify(ASSET, tmp, empty, tmp, empty);

        assertThat(result).isPresent();
        JsonNode summary = json.readTree(result.get());
        // ADDED: lists are intentionally empty
        assertThat(summary.path("classes").path("added")).isEmpty();
        assertThat(summary.path("classes").path("removed")).isEmpty();
        assertThat(summary.path("classes").path("modified")).isEmpty();
        // counts always populated
        assertThat(summary.path("classes").path("counts").path("added").asInt()).isEqualTo(2);
        assertThat(summary.path("classes").path("counts").path("removed").asInt()).isZero();
        assertThat(summary.path("properties").path("counts").path("added").asInt()).isEqualTo(1);
        assertThat(summary.path("tripleStats").path("added").asInt()).isPositive();
        assertThat(summary.path("tripleStats").path("removed").asInt()).isZero();
    }

    @Test
    void modifiedScenarioPopulatesListsAndTripleDetail() throws Exception {
        // ONLINE has Class A with old label; TMP has Class A with new label + new Class B
        Model online = ModelFactory.createDefaultModel();
        addClass(online, CLASS_A, "Vecchia label");

        Model tmp = ModelFactory.createDefaultModel();
        addClass(tmp, CLASS_A, "Nuova label");
        addClass(tmp, CLASS_B, "Classe B nuova");

        Model added = tmp.difference(online);
        Model removed = online.difference(tmp);

        Optional<String> result = classifier.classify(ASSET, added, removed, tmp, online);

        assertThat(result).isPresent();
        JsonNode summary = json.readTree(result.get());

        // class B is new in tmp, not in online -> classes.added
        assertThat(summary.path("classes").path("added"))
                .extracting(JsonNode::asText)
                .contains(CLASS_B);
        // class A is common, label changed -> classes.modified with triple-level detail
        JsonNode modified = summary.path("classes").path("modified");
        assertThat(modified).hasSize(1);
        assertThat(modified.get(0).path("iri").asText()).isEqualTo(CLASS_A);
        JsonNode triplesAdded = modified.get(0).path("triplesAdded");
        JsonNode triplesRemoved = modified.get(0).path("triplesRemoved");
        assertThat(triplesAdded).hasSize(1);
        assertThat(triplesRemoved).hasSize(1);
        assertThat(triplesAdded.get(0).path("p").asText()).isEqualTo(RDFS.label.getURI());
        assertThat(triplesAdded.get(0).path("o").path("value").asText()).isEqualTo("Nuova label");
        assertThat(triplesRemoved.get(0).path("o").path("value").asText()).isEqualTo("Vecchia label");
        assertThat(summary.path("classes").path("counts").path("added").asInt()).isEqualTo(1);
        assertThat(summary.path("classes").path("counts").path("modified").asInt()).isEqualTo(1);
    }

    @Test
    void classAndPropertyAreNotConflated() throws Exception {
        // A class whose label changes must end up only in classes.modified, not properties.modified
        Model online = ModelFactory.createDefaultModel();
        addClass(online, CLASS_A, "vecchia");
        addProperty(online, PROP_X);

        Model tmp = ModelFactory.createDefaultModel();
        addClass(tmp, CLASS_A, "nuova");
        addProperty(tmp, PROP_X);

        Model added = tmp.difference(online);
        Model removed = online.difference(tmp);

        Optional<String> result = classifier.classify(ASSET, added, removed, tmp, online);
        JsonNode summary = json.readTree(result.orElseThrow());

        assertThat(summary.path("classes").path("modified")).hasSize(1);
        assertThat(summary.path("classes").path("modified").get(0).path("iri").asText()).isEqualTo(CLASS_A);
        assertThat(summary.path("properties").path("modified")).isEmpty();
    }

    @Test
    void deprecatedFlagsAreCollected() throws Exception {
        Model online = ModelFactory.createDefaultModel();
        addClass(online, CLASS_A, "label");

        Model tmp = ModelFactory.createDefaultModel();
        addClass(tmp, CLASS_A, "label");
        // mark CLASS_A as deprecated in tmp
        tmp.add(tmp.getResource(CLASS_A), OWL.deprecated, tmp.createTypedLiteral(true));

        Model added = tmp.difference(online);
        Model removed = online.difference(tmp);

        Optional<String> result = classifier.classify(ASSET, added, removed, tmp, online);
        JsonNode summary = json.readTree(result.orElseThrow());

        assertThat(summary.path("deprecated").path("count").asInt()).isEqualTo(1);
        assertThat(summary.path("deprecated").path("items"))
                .extracting(JsonNode::asText)
                .contains(CLASS_A);
    }

    private static void addClass(Model model, String iri, String label) {
        Resource c = model.getResource(iri);
        model.add(c, RDF.type, OWL.Class);
        model.add(c, RDFS.label, label);
    }

    private static void addProperty(Model model, String iri) {
        Resource p = model.getResource(iri);
        model.add(p, RDF.type, OWL.DatatypeProperty);
    }
}
