package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SemanticAssetModelFactoryTest {

    public static final String REPO_URL = "http://repo";
    private final SemanticAssetModelFactory factory = new SemanticAssetModelFactory();

    @Test
    void canBuildControlledVocabularyModel() {
        String ttlFile = "src/test/resources/testdata/cv.ttl";

        ControlledVocabularyModel model = factory.createControlledVocabulary(ttlFile, REPO_URL);

        Resource resource = model.getMainResource();

        assertThat(resource).isNotNull();
        assertThat(resource.toString()).isEqualTo(
                "https://w3id.org/italia/controlled-vocabulary/classifications-for-accommodation-facilities/accommodation-star-rating");
    }

    @Test
    void canBuildOntologyModel() {
        String ttlFile = "src/test/resources/testdata/onto.ttl";

        OntologyModel model = factory.createOntology(ttlFile, REPO_URL);

        Resource resource = model.getMainResource();

        assertThat(resource.toString()).isEqualTo("https://w3id.org/italia/onto/CulturalHeritage");
    }

    @Test
    void canBuildSchemaModel() {
        String ttlFile = "src/test/resources/testdata/schema.ttl";

        SchemaModel model = factory.createSchema(ttlFile, REPO_URL);

        Resource resource = model.getMainResource();

        assertThat(resource.toString()).isEqualTo("https://w3id.org/italia/schema/person/v202108.01/person.oas3");
    }

    @Test
    void shouldFailForInvalidControlledVocabularyModel() {
        assertThatThrownBy(() -> factory.createControlledVocabulary("src/main/resources/application.properties",
            REPO_URL))
                .isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldFailForInvalidOntologyModel() {
        assertThatThrownBy(() -> factory.createOntology("src/main/resources/application.properties",
            REPO_URL))
                .isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldFailForInvalidSchemaModel() {
        assertThatThrownBy(
            () -> factory.createSchema("src/main/resources/application.properties", REPO_URL))
            .isInstanceOf(InvalidModelException.class);
    }
}