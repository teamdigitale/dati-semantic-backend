package it.gov.innovazione.ndc.harvester.model;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.SCHEMA;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createLangLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.contactPoint;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.downloadURL;
import static org.apache.jena.vocabulary.DCAT.keyword;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.conformsTo;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.format;
import static org.apache.jena.vocabulary.DCTerms.issued;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.publisher;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.subject;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.OWL.versionInfo;
import static org.apache.jena.vocabulary.RDFS.label;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;

import java.time.LocalDate;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaModelTest {

    private static final String TTL_FILE = "some-file";
    public static final String SCHEMA_IRI = "https://w3id.org/italia/schema/test";
    public static final String RIGHTS_HOLDER_IRI =
        "http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid";
    public static final String REPO_URL = "http://repo";
    private Model jenaModel;

    @BeforeEach
    void setupMockModel() {
        jenaModel = createDefaultModel();
        Resource agid = jenaModel
            .createResource(RIGHTS_HOLDER_IRI)
            .addProperty(FOAF.name, "agid");
        Resource distribution1 = jenaModel
            .createResource("distribution1")
            .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_JSON)
            .addProperty(accessURL, jenaModel.createResource("accessURL1"));
        Resource distribution2 = jenaModel
            .createResource("distribution2")
            .addProperty(format, EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE)
            .addProperty(accessURL, jenaModel.createResource("accessURL2"));
        jenaModel
            .createResource(SCHEMA_IRI)
            .addProperty(title, createLangLiteral("title", "en"))
            .addProperty(description, "description")
            .addProperty(modified, "2021-03-02")
            .addProperty(theme, createResource("theme"))
            .addProperty(rightsHolder, agid)
            .addProperty(distribution, distribution1)
            .addProperty(distribution, distribution2)
            .addProperty(RDF.type, createResource(SCHEMA.getTypeIri()))
            .addProperty(subject, createResource("subTheme1"))
            .addProperty(subject, createResource("subTheme2"))
            .addProperty(contactPoint, jenaModel.createResource("http://contactPoint")
                .addProperty(VCARD4.hasEmail, jenaModel.createResource("mailto:test@test.com")))
            .addProperty(publisher, jenaModel.createResource("http://publisher")
                .addProperty(FOAF.name, "publisher"))
            .addProperty(publisher, jenaModel.createResource("http://publisher2")
                .addProperty(FOAF.name, "publisher2"))
            .addProperty(issued, "2020-01-03")
            .addProperty(versionInfo, "1.0")
            .addProperty(keyword, "keyword1").addProperty(keyword, "keyword2")
            .addProperty(conformsTo, jenaModel.createResource("http://conformsTo")
                .addProperty(FOAF.name, "conformsTo"))
            .addProperty(conformsTo, jenaModel.createResource("http://conformsTo2")
                .addProperty(FOAF.name, "conformsTo2"))
            .addProperty(accessURL, jenaModel
                .createResource("accessURL1"))
            .addProperty(accessURL, jenaModel
                .createResource("accessURL2"))
            .addProperty(downloadURL, jenaModel
                .createResource("downloadURL1"))
            .addProperty(downloadURL, jenaModel
                .createResource("downloadURL2"))
            .addProperty(Admsapit.hasKeyClass,
                jenaModel.createResource("http://keyClasses1").addProperty(label, "keyClasses1"))
            .addProperty(Admsapit.hasKeyClass,
                jenaModel.createResource("http://keyClasses2").addProperty(label, "keyClasses2"));
    }

    @Test
    void shouldExtractMetadataWithIri() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getIri()).isEqualTo(SCHEMA_IRI);
    }

    @Test
    void shouldExtractWithRepoUrl() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getRepoUrl()).isEqualTo("http://repo");
    }

    @Test
    void shouldExtractMetadataWithTitleInEnglish() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getTitle()).isEqualTo("title");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutTitle() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        jenaModel.getResource(SCHEMA_IRI).removeAll(title);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithDescription() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getDescription()).isEqualTo("description");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDescription() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(description);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithSemanticAssetType() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getType()).isEqualTo(SCHEMA);
    }

    @Test
    void shouldExtractMetadataWithRightsHolder() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getRightsHolder().getIri()).isEqualTo(RIGHTS_HOLDER_IRI);
        assertThat(metadata.getRightsHolder().getSummary()).isEqualTo("agid");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutRightsHolder() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(rightsHolder);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithModified() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getModifiedOn()).isEqualTo(LocalDate.of(2021, 3, 2));
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutModified() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(modified);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getModifiedOn()).isNull();
    }

    @Test
    void shouldExtractMetadataWithTheme() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getThemes()).containsExactly("theme");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutTheme() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(theme);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithDistribution() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getDistributionUrls()).containsExactlyInAnyOrder("accessURL1");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDistribution() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(distribution);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithIssued() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getIssuedOn()).isEqualTo(LocalDate.of(2020, 1, 3));
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutIssued() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(issued);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getIssuedOn()).isNull();
    }

    @Test
    void shouldExtractMetadataWithVersionInfo() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getVersionInfo()).isEqualTo("1.0");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutVersionInfo() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        jenaModel.getResource(SCHEMA_IRI).removeAll(versionInfo);

        assertThatThrownBy(model::extractMetadata).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithKeywords() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getKeywords()).containsExactlyInAnyOrder("keyword1", "keyword2");
    }

    @Test
    void shouldExtractMetadataWithoutKeywords() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(keyword);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getKeywords()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithConformsTo() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);
        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getConformsTo()).hasSize(2);
        assertThat(metadata.getConformsTo()).containsExactlyInAnyOrder(
            NodeSummary.builder().iri("http://conformsTo").summary("conformsTo").build(),
            NodeSummary.builder().iri("http://conformsTo2").summary("conformsTo2").build()
        );
    }

    @Test
    void shouldExtractMetadataWithoutConformsTo() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(conformsTo);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getConformsTo()).isEmpty();
    }

    @Test
    void shouldExtractHasKeyClassProperty() {
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getKeyClasses()).hasSize(2);
        assertThat(metadata.getKeyClasses()).containsExactlyInAnyOrder(
            NodeSummary.builder().iri("http://keyClasses1").summary("keyClasses1").build(),
            NodeSummary.builder().iri("http://keyClasses2").summary("keyClasses2").build()
        );
    }

    @Test
    void shouldExtractMetadataWithoutHasKeyClassProperty() {
        jenaModel.getResource(SCHEMA_IRI).removeAll(Admsapit.hasKeyClass);
        SchemaModel model = new SchemaModel(jenaModel, TTL_FILE, REPO_URL);

        SemanticAssetMetadata metadata = model.extractMetadata();

        assertThat(metadata.getKeyClasses()).isEmpty();
    }
}
