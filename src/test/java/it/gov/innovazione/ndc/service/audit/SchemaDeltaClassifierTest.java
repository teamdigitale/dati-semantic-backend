package it.gov.innovazione.ndc.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaDeltaClassifierTest {

    private static final String DATASET = "https://w3id.org/example/schema/bando-isi";
    private static final String DISTRIBUTION = DATASET + "/distribution/oas3";

    private final SchemaDeltaClassifier classifier = new SchemaDeltaClassifier();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void supportsOnlySchema() {
        assertThat(classifier.supports(SemanticAssetType.SCHEMA)).isTrue();
        assertThat(classifier.supports(SemanticAssetType.ONTOLOGY)).isFalse();
        assertThat(classifier.supports(SemanticAssetType.CONTROLLED_VOCABULARY)).isFalse();
    }

    @Test
    void addedScenarioCountsDatasetsAndDistributions() throws Exception {
        Model tmp = ModelFactory.createDefaultModel();
        tmp.add(tmp.getResource(DATASET), RDF.type, DCAT.Dataset);
        tmp.add(tmp.getResource(DISTRIBUTION), RDF.type, DCAT.Distribution);
        Model empty = ModelFactory.createDefaultModel();

        Optional<String> result = classifier.classify(DATASET, tmp, empty, tmp, empty);

        JsonNode summary = json.readTree(result.orElseThrow());
        assertThat(summary.path("datasets").path("counts").path("added").asInt()).isEqualTo(1);
        assertThat(summary.path("distributions").path("counts").path("added").asInt()).isEqualTo(1);
        // lists empty on ADDED
        assertThat(summary.path("datasets").path("added")).isEmpty();
        assertThat(summary.path("distributions").path("added")).isEmpty();
    }
}
