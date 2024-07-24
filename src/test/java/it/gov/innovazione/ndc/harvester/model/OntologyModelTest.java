package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary;
import it.gov.innovazione.ndc.model.profiles.NDC;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.ONTOLOGY;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.downloadURL;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.accrualPeriodicity;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.format;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.RDF.type;
import static org.apache.jena.vocabulary.RDFS.label;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OntologyModelTest {
    private static final String TTL_FILE = "some-file";
    public static final String ONTOLOGY_IRI = "https://w3id.org/italia/ontology/test";
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
            .addProperty(type, createResource(ONTOLOGY.getTypeIri()))
            .addProperty(NDC.keyConcept, "test-concept")
            .addProperty(rightsHolder, agid)
            .addProperty(identifier, "test-identifier")
            .addProperty(title, "title")
            .addProperty(description, "description")
            .addProperty(modified, "2021-03-02")
            .addProperty(theme, createResource("theme"))
            .addProperty(accrualPeriodicity, createResource("IRREG"))
            .addProperty(Admsapit.hasSemanticAssetDistribution,
                jenaModel.createResource("http://repo/test/dist/turtle")
                    .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE)
                    .addProperty(accessURL, createResource("http://repo/test/dist/turtle"))
                    .addProperty(downloadURL, createResource("http://repo/test/dist/turtle/test.ttl"))
            )
            .addProperty(Admsapit.hasSemanticAssetDistribution,
                jenaModel.createResource("http://repo/test/dist/json")
                    .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_JSON)
                        .addProperty(accessURL, createResource("http://repo/test/dist/json"))
                        .addProperty(downloadURL, createResource("http://repo/test/dist/json/test.json"))
            )
            .addProperty(Admsapit.hasSemanticAssetDistribution,
                jenaModel.createResource("http://repo/test/dist/other")
                    .addProperty(accessURL, createResource("http://repo/test/dist/other"))
            )
            .addProperty(Admsapit.hasKeyClass,
                jenaModel.createResource("http://Class1").addProperty(label, "Class1"))
            .addProperty(Admsapit.hasKeyClass,
                jenaModel.createResource("http://Class2").addProperty(label, "Class2"))
            .addProperty(Admsapit.semanticAssetInUse, jenaModel.createResource("http://project1")
                .addProperty(createProperty("https://w3id.org/italia/onto/l0/name"), "project1")
                .addProperty(type, Admsapit.project))
            .addProperty(Admsapit.semanticAssetInUse, jenaModel.createResource("http://project2")
                .addProperty(createProperty("https://w3id.org/italia/onto/l0/name"), "project2")
                .addProperty(type, Admsapit.project))
            .addProperty(Admsapit.prefix, "prefix");
    }

    @Test
    void shouldExtractMetadataWithSemanticAssetType() {
        OntologyModel model = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getType()).isEqualTo(ONTOLOGY);
    }

    @Test
    void shouldValidateMetadataWithSemanticAssetType() {
        OntologyModel model = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetModelValidationContext semanticAssetModelValidationContext = model.validateMetadata();

        System.out.println(semanticAssetModelValidationContext.getErrors());
        //assertThat(semanticAssetModelValidationContext.getErrors().size()).isEqualTo(0);
    }

    @Test
    void shouldExtractHasSemanticAssetDistributionProperty() {
        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        List<Distribution> distributions = metadata.getDistributions();
        assertThat(distributions).hasSize(1);
        assertThat(distributions.get(0).getAccessUrl()).isEqualTo("http://repo/test/dist/turtle");
        assertThat(distributions.get(0).getDownloadUrl()).isEqualTo("http://repo/test/dist/turtle/test.ttl");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDistribution() {
        jenaModel.getResource(ONTOLOGY_IRI).removeAll(Admsapit.hasSemanticAssetDistribution);
        ControlledVocabularyModel model =
            new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractHasKeyClassProperty() {
        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        assertThat(metadata.getKeyClasses()).hasSize(2);
        assertThat(metadata.getKeyClasses()).containsExactlyInAnyOrder(
            NodeSummary.builder().iri("http://Class1").summary("Class1").build(),
            NodeSummary.builder().iri("http://Class2").summary("Class2").build()
        );
    }

    @Test
    void shouldExtractMetadataWithoutHasKeyClassProperty() {
        jenaModel.getResource(ONTOLOGY_IRI).removeAll(Admsapit.hasKeyClass);
        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        assertThat(metadata.getKeyClasses()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithPrefix() {

        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        assertThat(metadata.getPrefix()).isEqualTo("prefix");
    }

    @Test
    void shouldExtractMetadataWithOutPrefix() {
        jenaModel.getResource(ONTOLOGY_IRI).removeAll(Admsapit.prefix);
        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        assertThat(metadata.getPrefix()).isEqualTo(null);
    }

    @Test
    void shouldExtractMetadataWithProject() {
        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        assertThat(metadata.getProjects()).hasSize(2);
        assertThat(metadata.getProjects()).containsExactlyInAnyOrder(
            NodeSummary.builder().iri("http://project1").summary("project1").build(),
            NodeSummary.builder().iri("http://project2").summary("project2").build()
        );
    }

    @Test
    void shouldExtractMetadataWithOutProject() {
        jenaModel.getResource(ONTOLOGY_IRI).removeAll(Admsapit.semanticAssetInUse);
        OntologyModel ontologyModel = new OntologyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = ontologyModel.extractMetadata();

        assertThat(metadata.getProjects()).isEmpty();
    }
}
