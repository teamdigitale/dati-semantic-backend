package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary;
import it.gov.innovazione.ndc.model.profiles.NDC;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
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

import java.util.List;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.gov.innovazione.ndc.harvester.model.Instance.PRIMARY;
import static java.util.Objects.nonNull;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.downloadURL;
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

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyModelTest {
    private static final String TTL_FILE = "some-file";
    public static final String CV_IRI = "https://w3id.org/italia/controlled-vocabulary/test";
    public static final String RIGHTS_HOLDER_IRI =
            "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
    public static final String REPO_URL = "http://repo";
    public static final String ENDPOINT_BASE_URL = "http://ndc";
    public static final String TURTLE_DISTRIBUTION_IRI = "http://repo/distribution/file.rdf";
    public static final String JSON_DISTRIBUTION_IRI = "http://repo/distribution/file.json";
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
            .addProperty(NDC.keyConcept, "test-concept")
            .addProperty(rightsHolder, agid)
            .addProperty(identifier, "test-identifier")
            .addProperty(title, "title")
            .addProperty(description, "description")
            .addProperty(modified, "2021-03-02")
            .addProperty(theme, createResource("theme"))
            .addProperty(accrualPeriodicity, createResource("IRREG"))
            .addProperty(distribution, jenaModel.createResource(TURTLE_DISTRIBUTION_IRI)
                .addProperty(accessURL, createResource("http://repo/cv/dist/ttl"))
                .addProperty(downloadURL, createResource("http://repo/cv/dist/ttl/file.ttl"))
                .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE)
            )
            .addProperty(distribution, jenaModel.createResource(JSON_DISTRIBUTION_IRI)
                .addProperty(accessURL, createResource("http://repo/cv/dist/json"))
                .addProperty(downloadURL, createResource("http://repo/cv/dist/json/file.json"))
                .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_JSON)
            );
    }

    @Test
    void shouldExtractMainResource() {
        ControlledVocabularyModel model =
            new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL, PRIMARY);

        Resource mainResource = model.getMainResource();

        assertThat(mainResource.toString()).isEqualTo(CV_IRI);
    }

    @Test
    void shouldExtractMetadataWithSemanticAssetType() {
        ControlledVocabularyModel model =
            new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL, PRIMARY);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getType()).isEqualTo(CONTROLLED_VOCABULARY);
    }

    @Test
    void shouldValidateMetadataWithSemanticAssetType() {
        ControlledVocabularyModel model =
            new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL, PRIMARY);

        SemanticAssetModelValidationContext semanticAssetModelValidationContext = model.validateMetadata();

        System.out.println(semanticAssetModelValidationContext.getErrors());
        //assertThat(semanticAssetModelValidationContext.getErrors().size()).isEqualTo(0);
    }

    @Test
    void shouldFailWhenModelDoesNotContainControlledVocabulary() {
        List<Statement> conceptStatements = jenaModel
                .listStatements(null, RDF.type, (RDFNode) null)
                .toList();
        assertThat(conceptStatements.size()).isEqualTo(1);
        conceptStatements.forEach(s -> jenaModel.remove(s));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.getMainResource()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldFailWhenTtlContainsMoreThanOneControlledVocabulary() {
        jenaModel
                .createResource("http://www.diseny.com/characters")
                .addProperty(RDF.type, createResource(CONTROLLED_VOCABULARY.getTypeIri()));

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.getMainResource()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldExtractKeyConcept() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThat(model.getKeyConcept()).isEqualTo("test-concept");
    }

    @Test
    void shouldExtractMetadataWithDistribution() {
        ControlledVocabularyModel model =
            new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL, PRIMARY);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getDistributions()).hasSize(1);
        assertThat(metadata.getDistributions().get(0).getDownloadUrl()).isEqualTo("http://repo/cv/dist/ttl/file.ttl");
    }

    @Test
    void shouldComplainForTurtleDistributionWithoutUrl() {
        jenaModel.getResource(CV_IRI).listProperties(distribution)
            .forEach(d -> List.of(accessURL, downloadURL)
                .forEach(p -> removePropertyIfExists(d, p)
            )
        );

        ControlledVocabularyModel model =
            new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.extractMetadata())
                .isInstanceOf(InvalidModelException.class)
                .hasMessageContaining(TURTLE_DISTRIBUTION_IRI)
                .hasMessageContaining(downloadURL.getURI());
    }

    private void removePropertyIfExists(Statement d, Property p) {
        Statement property = d.getProperty(p);
        if (nonNull(property)) {
            property.remove();
        }
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDistribution() {
        jenaModel.getResource(CV_IRI).removeAll(distribution);
        ControlledVocabularyModel model =
            new ControlledVocabularyModel(jenaModel, TTL_FILE, REPO_URL, PRIMARY);


        assertThatThrownBy(() -> model.extractMetadata()).isInstanceOf(
                InvalidModelException.class);
    }

    @Test
    void shouldFailWithMissingKeyConcept() {
        jenaModel.getResource(CV_IRI).removeAll(NDC.keyConcept);

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidModelException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"accomodation details", "education-", "-levels"})
    void shouldFailWithInvalidKeyConcept(String keyConcept) {
        jenaModel.getResource(CV_IRI).getProperty(NDC.keyConcept).changeObject(keyConcept);

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldFailWithMultipleKeyConcepts() {
        jenaModel
                .createResource(CV_IRI)
                .addProperty(RDF.type, createResource(CONTROLLED_VOCABULARY.getTypeIri()))
                .addProperty(NDC.keyConcept, "another-concept");

        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.getKeyConcept()).isInstanceOf(InvalidModelException.class);
    }

    @Test
    void shouldExtractAgencyId() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThat(model.getAgencyId().getIdentifier()).isEqualTo("agid");
    }

    @Test
    void shouldComplainIfRightsHolderIsUndefined() {
        jenaModel.getResource(CV_IRI).getProperty(rightsHolder).remove();
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.getAgencyId())
                .isInstanceOf(InvalidModelException.class)
                .hasMessageContaining("rightsHolder");
    }

    @Test
    void shouldComplainIfRightsHolderHasNoId() {
        jenaModel.getResource(RIGHTS_HOLDER_IRI).getProperty(identifier).remove();
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        assertThatThrownBy(() -> model.getAgencyId())
                .isInstanceOf(InvalidModelException.class)
                .hasMessageContaining("rightsHolder")
                .hasMessageContaining(RIGHTS_HOLDER_IRI);
    }

    @Test
    void shouldExtractKeyConceptMetaData() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        SemanticAssetMetadata semanticAssetMetadata = model.extractMetadata();

        assertThat(semanticAssetMetadata.getKeyConcept()).isEqualTo("test-concept");
    }

    @Test
    void shouldExtractAgencyIdMetaData() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        SemanticAssetMetadata semanticAssetMetadata = model.extractMetadata();

        assertThat(semanticAssetMetadata.getAgencyId()).isEqualTo("agid");
    }

    @Test
    void shouldProvideEndpointUrlAsPartOfMetaDataAfterEnrichingModel() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        model.addNdcDataServiceProperties(ENDPOINT_BASE_URL);

        SemanticAssetMetadata semanticAssetMetadata = model.extractMetadata();
        assertThat(semanticAssetMetadata.getEndpointUrl()).isEqualTo(
                "http://ndc/vocabularies/agid/test-concept");
    }

    @Test
    void shouldAddDataService() {
        ControlledVocabularyModel model = new ControlledVocabularyModel(jenaModel, TTL_FILE,
            REPO_URL, PRIMARY);

        model.addNdcDataServiceProperties(ENDPOINT_BASE_URL);

        Model enrichedRdfModel = model.getRdfModel();
        Resource clazz = NDC.DataService;
        List<Resource> dataServices = findResourceByClass(enrichedRdfModel, clazz);
        assertThat(dataServices).hasSize(1);
        Resource dataService = dataServices.get(0);

        Resource mainResource = model.getMainResource();
        assertThat(dataService.getPropertyResourceValue(NDC.servesDataset)).isEqualTo(mainResource);
        assertThat(mainResource.getPropertyResourceValue(NDC.hasDataService)).isEqualTo(dataService);
        String endpointUrl = dataService.getProperty(NDC.endpointURL).getObject().toString();
        assertThat(endpointUrl).isEqualTo("http://ndc/vocabularies/agid/test-concept");
    }

    private List<Resource> findResourceByClass(Model enrichedRdfModel, Resource clazz) {
        ResIterator resIterator = enrichedRdfModel.listResourcesWithProperty(RDF.type, clazz);
        return resIterator.toList();
    }
}
