package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.KEY_CONCEPT_IRI;
import static it.teamdigitale.ndc.harvester.model.OntologyModel.ADMSAPIT_DISTRIBUTION_PROPERTY;
import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import org.apache.jena.rdf.model.Model;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import org.apache.jena.rdf.model.Resource;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.accrualPeriodicity;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.title;
import org.apache.jena.vocabulary.RDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OntologyModelTest {
    private static final String TTL_FILE = "some-file";
    public static final String ONTOLOGY_IRI = "https://w3id.org/italia/controlled-vocabulary/test";
    public static final String RIGHTS_HOLDER_IRI =
        "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
    public static final String REPO_URL = "http://repo";
    private Model jenaModel;

    @BeforeEach
    void setupMockModel() {
        jenaModel = createDefaultModel();
        Resource agid = jenaModel
            .createResource(RIGHTS_HOLDER_IRI)
            .addProperty(identifier, "agid");
        jenaModel
            .createResource(ONTOLOGY_IRI)
            .addProperty(RDF.type, createResource(ONTOLOGY.getTypeIri()))
            .addProperty(createProperty(KEY_CONCEPT_IRI), "test-concept")
            .addProperty(rightsHolder, agid)
            .addProperty(identifier, "test-identifier")
            .addProperty(title, "title")
            .addProperty(description, "description")
            .addProperty(modified, "2021-03-02")
            .addProperty(theme, createResource("theme"))
            .addProperty(accrualPeriodicity, createResource("IRREG"))
            .addProperty(ADMSAPIT_DISTRIBUTION_PROPERTY, createResource("rdf file path"))
            .addProperty(ADMSAPIT_DISTRIBUTION_PROPERTY, createResource("ttl file path"));
    }

    @Test
    void shouldExtractHasSemanticAssetDistributionProperty() {
        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        assertThat(metadata.getDistributionUrls()).containsExactlyInAnyOrder("rdf file path", "ttl file path");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDistribution() {
        jenaModel.getResource(ONTOLOGY_IRI).removeAll(ADMSAPIT_DISTRIBUTION_PROPERTY);
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }
}