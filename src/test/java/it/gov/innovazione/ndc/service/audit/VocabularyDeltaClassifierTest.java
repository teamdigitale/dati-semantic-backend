package it.gov.innovazione.ndc.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VocabularyDeltaClassifierTest {

    private static final String SCHEME = "https://w3id.org/example/vocab/thesaurus";
    private static final String CONCEPT_1 = "https://w3id.org/example/vocab/concept/1";
    private static final String CONCEPT_2 = "https://w3id.org/example/vocab/concept/2";

    private final VocabularyDeltaClassifier classifier = new VocabularyDeltaClassifier();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void supportsOnlyVocabulary() {
        assertThat(classifier.supports(SemanticAssetType.CONTROLLED_VOCABULARY)).isTrue();
        assertThat(classifier.supports(SemanticAssetType.ONTOLOGY)).isFalse();
        assertThat(classifier.supports(SemanticAssetType.SCHEMA)).isFalse();
    }

    @Test
    void addedScenarioCountsConceptsAndSchemes() throws Exception {
        Model tmp = ModelFactory.createDefaultModel();
        tmp.add(tmp.getResource(SCHEME), RDF.type, SKOS.ConceptScheme);
        tmp.add(tmp.getResource(CONCEPT_1), RDF.type, SKOS.Concept);
        tmp.add(tmp.getResource(CONCEPT_2), RDF.type, SKOS.Concept);
        Model empty = ModelFactory.createDefaultModel();

        Optional<String> result = classifier.classify(SCHEME, tmp, empty, tmp, empty);

        JsonNode summary = json.readTree(result.orElseThrow());
        assertThat(summary.path("concepts").path("counts").path("added").asInt()).isEqualTo(2);
        assertThat(summary.path("schemes").path("counts").path("added").asInt()).isEqualTo(1);
        assertThat(summary.path("collections").path("counts").path("added").asInt()).isZero();
        // lists empty for ADDED
        assertThat(summary.path("concepts").path("added")).isEmpty();
    }

    @Test
    void modifiedScenarioReportsConceptLabelChange() throws Exception {
        Model online = ModelFactory.createDefaultModel();
        online.add(online.getResource(SCHEME), RDF.type, SKOS.ConceptScheme);
        online.add(online.getResource(CONCEPT_1), RDF.type, SKOS.Concept);
        online.add(online.getResource(CONCEPT_1), SKOS.prefLabel, "Vecchia label");

        Model tmp = ModelFactory.createDefaultModel();
        tmp.add(tmp.getResource(SCHEME), RDF.type, SKOS.ConceptScheme);
        tmp.add(tmp.getResource(CONCEPT_1), RDF.type, SKOS.Concept);
        tmp.add(tmp.getResource(CONCEPT_1), SKOS.prefLabel, "Nuova label");

        Model added = tmp.difference(online);
        Model removed = online.difference(tmp);

        Optional<String> result = classifier.classify(SCHEME, added, removed, tmp, online);

        JsonNode summary = json.readTree(result.orElseThrow());
        JsonNode conceptsModified = summary.path("concepts").path("modified");
        assertThat(conceptsModified).hasSize(1);
        assertThat(conceptsModified.get(0).path("iri").asText()).isEqualTo(CONCEPT_1);
        assertThat(conceptsModified.get(0).path("triplesAdded").get(0).path("o").path("value").asText())
                .isEqualTo("Nuova label");
        assertThat(conceptsModified.get(0).path("triplesRemoved").get(0).path("o").path("value").asText())
                .isEqualTo("Vecchia label");
    }
}
