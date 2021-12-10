package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.KEY_CONCEPT_IRI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.accrualPeriodicity;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.format;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.model.profiles.EuropePublicationVocabulary;

import java.util.List;

import it.teamdigitale.ndc.model.profiles.NDC;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyModelTest {
    private static final String TTL_FILE = "some-file";
    public static final String CV_IRI = "https://w3id.org/italia/controlled-vocabulary/test";
    public static final String RIGHTS_HOLDER_IRI =
            "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
    public static final String REPO_URL = "http://repo";
    public static final String ENDPOINT_BASE_URL = "http://ndc";
    private Model jenaModel;

    @BeforeEach
    void setupMockModel() {
        jenaModel = createDefaultModel();
        Resource agid = jenaModel
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
            .addProperty(distribution, jenaModel.createResource("rdf file path")
                .addProperty(accessURL, createResource("http://repo/file.rdf"))
                .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE)
            )
            .addProperty(distribution, jenaModel.createResource("json file path")
                .addProperty(accessURL, createResource("http://repo/file.json"))
                .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_JSON)
            );

    }

    @Test
    void shouldExtractMainResource() {
        ControlledVocabularyModel model =
                new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL);

        Resource mainResource = model.getMainResource();

        assertThat(mainResource.toString()).isEqualTo(CV_IRI);
    }

    @Test
    void shouldExtractMetadataWithSemanticAssetType() {
        ControlledVocabularyModel model =
                new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getType()).isEqualTo(CONTROLLED_VOCABULARY);
    }

    @Test
    void shouldFailWhenModelDoesNotContainControlledVocabulary() {
        List<Statement> conceptStatements = jenaModel
                .listStatements(null, RDF.type, (RDFNode) null)
                .toList();
        assertThat(conceptStatements.size()).isEqualTo(1);
        conceptStatements.forEach(s -> jenaModel.remove(s));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        assertThatThrownBy(() -> model.getMainResource()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldFailWhenTtlContainsMoreThanOneControlledVocabulary() {
        jenaModel
                .createResource("http://www.diseny.com/characters")
                .addProperty(RDF.type, createResource(CONTROLLED_VOCABULARY.getTypeIri()));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        assertThatThrownBy(() -> model.getMainResource()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldExtractKeyConcept() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        assertThat(model.getKeyConcept()).isEqualTo("test-concept");
    }

    @Test
    void shouldExtractMetadataWithDistribution() {
        ControlledVocabularyModel model =
                new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getDistributionUrls()).containsExactlyInAnyOrder(
                "http://repo/file.rdf");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDistribution() {
        jenaModel.getResource(CV_IRI).removeAll(distribution);
        ControlledVocabularyModel model =
                new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL);


        assertThatThrownBy(() -> model.extractMetadata()).isInstanceOf(
                InvalidModelException.class);
    }

    @Test
    void shouldFailWithMissingKeyConcept() {
        jenaModel.getResource(CV_IRI).removeAll(createProperty(KEY_CONCEPT_IRI));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidModelException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"accomodation details", "education-", "-levels"})
    void shouldFailWithInvalidKeyConcept(String keyConcept) {
        jenaModel.getResource(CV_IRI).getProperty(createProperty(KEY_CONCEPT_IRI)).changeObject(keyConcept);

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldFailWithMultipleKeyConcepts() {
        jenaModel
                .createResource(CV_IRI)
                .addProperty(RDF.type, createResource(CONTROLLED_VOCABULARY.getTypeIri()))
                .addProperty(createProperty(KEY_CONCEPT_IRI), "another-concept");

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldExtractAgencyId() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        assertThat(model.getAgencyId()).isEqualTo("agid");
    }

    @Test
    void shouldExtractKeyConceptMetaData() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        SemanticAssetMetadata semanticAssetMetadata = model.extractMetadata();

        assertThat(semanticAssetMetadata.getKeyConcept()).isEqualTo("test-concept");
    }

    @Test
    void shouldExtractAgencyIdMetaData() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        SemanticAssetMetadata semanticAssetMetadata = model.extractMetadata();

        assertThat(semanticAssetMetadata.getAgencyId()).isEqualTo("agid");
    }

    @Test
    void shouldProvideEndpointUrlAsPartOfMetaDataAfterEnrichingModel() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        model.addNdcDataServiceProperties(ENDPOINT_BASE_URL);

        SemanticAssetMetadata semanticAssetMetadata = model.extractMetadata();
        assertThat(semanticAssetMetadata.getEndpointUrl()).isEqualTo(
                "http://ndc/vocabularies/agid/test-concept");
    }

    @Test
    void shouldAddNdcDataServiceProperties() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
                REPO_URL);

        model.addNdcDataServiceProperties(ENDPOINT_BASE_URL);

        Model enrichedRdfModel = model.getRdfModel();
        Resource clazz = NDC.DataService;
        List<Resource> dataServices = findResourceByClass(enrichedRdfModel, clazz);
        assertThat(dataServices).hasSize(1);
        Resource dataService = dataServices.get(0);

        Resource servedDataset = dataService.getPropertyResourceValue(NDC.servesDataset);
        assertThat(servedDataset).isNotNull();
        assertThat(servedDataset).isEqualTo(model.getMainResource());
    }

    private List<Resource> findResourceByClass(Model enrichedRdfModel, Resource clazz) {
        ResIterator resIterator = enrichedRdfModel.listResourcesWithProperty(RDF.type, clazz);
        return resIterator.toList();
    }
}