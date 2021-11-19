package it.teamdigitale.ndc.harvester;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

class SemanticAssetsParserTest {

    @Test
    void shouldReturnControlledVocabularyFromTtlFile() {
        String ttlFile = "src/test/resources/testdata/cv.ttl";
        SemanticAssetsParser semanticAssetsParser = new SemanticAssetsParser();

        Resource controlledVocabulary = semanticAssetsParser.getControlledVocabulary(ttlFile);

        assertThat(controlledVocabulary.getURI())
            .isEqualTo("https://w3id.org/italia/controlled-vocabulary/"
                + "classifications-for-accommodation-facilities/accommodation-star-rating");
    }

    @Test
    void shouldFailWhenTtlDoNotContainControlledVocabulary() {
        String ttlFile = "src/test/resources/testdata/no-cv.ttl";
        SemanticAssetsParser semanticAssetsParser = new SemanticAssetsParser();

        assertThrows(AssertionError.class,
            () -> semanticAssetsParser.getControlledVocabulary(ttlFile));
    }

    @Test
    void shouldFailWhenTtlContainsMoreThanOneControlledVocabulary() {
        String ttlFile = "src/test/resources/testdata/two-cv.ttl";
        SemanticAssetsParser semanticAssetsParser = new SemanticAssetsParser();

        assertThrows(AssertionError.class,
            () -> semanticAssetsParser.getControlledVocabulary(ttlFile));
    }

    @Test
    void shouldReturnKeyConcept() {
        Resource controlledVocabulary = createDefaultModel()
            .createResource("https://w3id.org/italia/controlled-vocabulary/test")
            .addProperty(createProperty("https://w3id.org/italia/onto/NDC/keyConcept"),
                "someValue");
        SemanticAssetsParser semanticAssetsParser = new SemanticAssetsParser();

        String keyConcept = semanticAssetsParser.getKeyConcept(controlledVocabulary);

        assertThat(keyConcept).isEqualTo("someValue");
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
        Resource agid = defaultModel.createResource(
                "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid")
            .addProperty(identifier, id);
        return defaultModel
            .createResource("https://w3id.org/italia/controlled-vocabulary/test")
            .addProperty(rightsHolder, agid);
    }
}