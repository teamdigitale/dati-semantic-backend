package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.KEY_CONCEPT_IRI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseSemanticAssetModelTest {

    private static final String TTL_FILE = "some-file";
    public static final String CV_IRI = "https://w3id.org/italia/controlled-vocabulary/test";
    public static final String RIGHTS_HOLDER_IRI =
        "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
    private Model jenaModel;
    private BaseSemanticAssetModel semanticAssetModel;

    @BeforeEach
    void setupMockModel() {
        jenaModel = createDefaultModel();
        Resource agid =
            jenaModel
                .createResource(RIGHTS_HOLDER_IRI)
                .addProperty(identifier, "agid");
        jenaModel
            .createResource(CV_IRI)
            .addProperty(RDF.type,
                createResource(CONTROLLED_VOCABULARY.getTypeIri()))
            .addProperty(createProperty(KEY_CONCEPT_IRI), "test-concept")
            .addProperty(identifier, "test-identifier")
            .addProperty(rightsHolder, agid);
        semanticAssetModel = new TestBaseSemanticAssetModel(jenaModel, TTL_FILE);
    }

    @Test
    void shouldExtractMetadataWithIri() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getIri()).isEqualTo(CV_IRI);
    }


    @Test
    void shouldExtractMetadataWithIdentifier() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getIdentifier()).isEqualTo("test-identifier");
    }

    @Test
    void shouldExtractMetadataWithSemanticAssetType() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getType()).isEqualTo(CONTROLLED_VOCABULARY);
    }

    @Test
    void shouldExtractMetadataWithRightsHolder() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getRightsHolder()).isEqualTo(RIGHTS_HOLDER_IRI);
    }

    private static class TestBaseSemanticAssetModel extends BaseSemanticAssetModel {

        public TestBaseSemanticAssetModel(Model coreModel, String source) {
            super(coreModel, source);
        }

        @Override
        protected String getMainResourceTypeIri() {
            return CONTROLLED_VOCABULARY.getTypeIri();
        }
    }
}