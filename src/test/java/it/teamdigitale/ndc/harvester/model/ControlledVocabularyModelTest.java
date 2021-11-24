package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.KEY_CONCEPT_IRI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.accrualPeriodicity;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyModelTest {
    private static final String TTL_FILE = "some-file";
    public static final String CV_IRI = "https://w3id.org/italia/controlled-vocabulary/test";
    public static final String RIGHTS_HOLDER_IRI =
            "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
    private Model jenaModel;

    @BeforeEach
    void setupMockModel() {
        jenaModel = createDefaultModel();
        Resource agid =
                jenaModel
                        .createResource(RIGHTS_HOLDER_IRI)
                        .addProperty(identifier, "agid");
        jenaModel
                .createResource(CV_IRI)
                .addProperty(RDF.type, createResource(CONTROLLED_VOCABULARY.getTypeIri()))
                .addProperty(createProperty(KEY_CONCEPT_IRI), "test-concept")
                .addProperty(rightsHolder, agid)
                .addProperty(identifier, "test-identifier")
                .addProperty(title, "title")
                .addProperty(description, "description")
                .addProperty(modified, "2021-03-02")
                .addProperty(theme, createResource("theme"))
                .addProperty(accrualPeriodicity, createResource("IRREG"))
                .addProperty(distribution, createResource("rdf file path")).addProperty(distribution, createResource("ttl file path"));

    }

    @Test
    void shouldExtractMainResource() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        Resource mainResource = model.getMainResource();

        assertThat(mainResource.toString()).isEqualTo(CV_IRI);
    }

    @Test
    void shouldFailWhenModelDoesNotContainControlledVocabulary() {
        List<Statement> conceptStatements = jenaModel
                .listStatements(null, RDF.type, (RDFNode) null)
                .toList();
        assertThat(conceptStatements.size()).isEqualTo(1);
        conceptStatements.forEach(s -> jenaModel.remove(s));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        assertThatThrownBy(() -> model.getMainResource()).isInstanceOf(InvalidAssetException.class);
    }

    @Test
    void shouldFailWhenTtlContainsMoreThanOneControlledVocabulary() {
        jenaModel
                .createResource("http://www.diseny.com/characters")
                .addProperty(RDF.type, createResource(CONTROLLED_VOCABULARY.getTypeIri()));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        assertThatThrownBy(() -> model.getMainResource()).isInstanceOf(InvalidAssetException.class);
    }

    @Test
    void shouldExtractKeyConcept() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        assertThat(model.getKeyConcept()).isEqualTo("test-concept");
    }

    @Test
    void shouldFailWithMissingKeyConcept() {
        List<Statement> conceptStatements = jenaModel
                .listStatements(null, createProperty(KEY_CONCEPT_IRI), (String) null)
                .toList();
        assertThat(conceptStatements.size()).isEqualTo(1);
        conceptStatements.forEach(s -> jenaModel.remove(s));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidAssetException.class);
    }

    @Test
    void shouldFailWithMultipleKeyConcepts() {
        jenaModel
                .createResource(CV_IRI)
                .addProperty(RDF.type, createResource(CONTROLLED_VOCABULARY.getTypeIri()))
                .addProperty(createProperty(KEY_CONCEPT_IRI), "another-concept");

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidAssetException.class);
    }

    @Test
    void shouldExtractRightsHolder() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        assertThat(model.getRightsHolderId()).isEqualTo("agid");
    }

    @Test
    void shouldExtractKeyConceptMetaData() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);

        SemanticAssetMetadata semanticAssetMetadata = model.extractMetadata();

        assertThat(semanticAssetMetadata.getKeyConcept()).isEqualTo("test-concept");
    }

    @Test
    void shouldExtractDataService() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE);
        assertThat(model.dataServiceResource().toString()).isEqualTo("<ModelCom   {https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept @https://w3id.org/italia/onto/ndc-profile/servesDataset https://w3id.org/italia/controlled-vocabulary/test; https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept @https://w3id.org/italia/onto/ndc-profile/endpointUrl https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept; https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept @http://www.w3.org/1999/02/22-rdf-syntax-ns#type https://w3id.org/italia/onto/ndc-profile/DataService} |  [https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept, https://w3id.org/italia/onto/ndc-profile/servesDataset, https://w3id.org/italia/controlled-vocabulary/test] [https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept, https://w3id.org/italia/onto/ndc-profile/endpointUrl, https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept] [https://w3id.org/italia/controlled-vocabulary-api/agid/test-concept, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, https://w3id.org/italia/onto/ndc-profile/DataService]>");
    }

}
