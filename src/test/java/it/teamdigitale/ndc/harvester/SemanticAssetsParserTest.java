package it.teamdigitale.ndc.harvester;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.assertj.core.api.Assertions.assertThat;

class SemanticAssetsParserTest {

    @Test
    void shouldReturnOntologyFromTtlFile() {
        String ttlFile = "src/test/resources/testdata/onto.ttl";
        SemanticAssetsParser semanticAssetsParser = new SemanticAssetsParser();

        Resource controlledVocabulary = semanticAssetsParser.getOntology(ttlFile);

        assertThat(controlledVocabulary.getURI()).isEqualTo("https://w3id.org/italia/onto/CulturalHeritage");
    }

    @Test
    void shouldReturnRightsHolderId() {
        Resource controlledVocabulary = createControlledVocabularyWithRightsHolder("agid");
        SemanticAssetsParser semanticAssetsParser = new SemanticAssetsParser();

        String id = semanticAssetsParser.getRightsHolderId(controlledVocabulary);

        assertThat(id).isEqualTo("agid");
    }

    private Resource createControlledVocabularyWithRightsHolder(String id) {
        Model defaultModel = createDefaultModel();
        Resource agid =
                defaultModel
                        .createResource("http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid")
                        .addProperty(identifier, id);
        return defaultModel
                .createResource("https://w3id.org/italia/controlled-vocabulary/test")
                .addProperty(rightsHolder, agid);
    }
}
