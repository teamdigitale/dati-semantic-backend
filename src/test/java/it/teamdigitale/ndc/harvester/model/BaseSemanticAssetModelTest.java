package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.KEY_CONCEPT_IRI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createLangLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.contactPoint;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.keyword;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.accrualPeriodicity;
import static org.apache.jena.vocabulary.DCTerms.conformsTo;
import static org.apache.jena.vocabulary.DCTerms.creator;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.issued;
import static org.apache.jena.vocabulary.DCTerms.language;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.publisher;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.subject;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.OWL.versionInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PropertyNotFoundException;
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
            .addProperty(rightsHolder, agid)
            .addProperty(title, createLangLiteral("title", "en"))
            .addProperty(title, createLangLiteral("titolo", "it"))
            .addProperty(title, createLangLiteral("french title", "fr"))
            .addProperty(description, "description")
            .addProperty(modified, "2021-03-02")
            .addProperty(theme, createResource("theme"))
            .addProperty(accrualPeriodicity, createResource("IRREG"))
            .addProperty(distribution, createResource("rdf file path"))
            .addProperty(distribution, createResource("ttl file path"))
            .addProperty(subject, createResource("subTheme1"))
            .addProperty(subject, createResource("subTheme2"))
            .addProperty(contactPoint, createResource("Agid"))
            .addProperty(publisher, createResource("Agid"))
            .addProperty(creator, createResource("stlab"))
            .addProperty(versionInfo, "1.0")
            .addProperty(issued, "2021-02-01")
            .addProperty(language, createResource("ENG"))
            .addProperty(keyword, "keyword1").addProperty(keyword, "keyword2")
            .addProperty(conformsTo, createResource("SKOS"));
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
    void shouldFailWhenExtractingMetadataWithOutIdentifier() {
        jenaModel.getResource(CV_IRI).removeAll(identifier);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithSemanticAssetType() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getType()).isEqualTo(CONTROLLED_VOCABULARY);
    }

    @Test
    void shouldExtractMetadataWithTitleInItalian() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getTitle()).isEqualTo("titolo");
    }

    @Test
    void shouldExtractMetadataWithTitleInEnglish() {
        jenaModel.getResource(CV_IRI).removeAll(title);
        jenaModel.getResource(CV_IRI).addProperty(title, "title");
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getTitle()).isEqualTo("title");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutTitle() {
        jenaModel.getResource(CV_IRI).removeAll(title);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithDescription() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getDescription()).isEqualTo("description");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDescription() {
        jenaModel.getResource(CV_IRI).removeAll(description);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithModified() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getModified()).isEqualTo(LocalDate.of(2021, 3, 2));
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutModified() {
        jenaModel.getResource(CV_IRI).removeAll(modified);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithTheme() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getTheme()).containsExactly("theme");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutTheme() {
        jenaModel.getResource(CV_IRI).removeAll(theme);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithRightsHolder() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getRightsHolder()).isEqualTo(RIGHTS_HOLDER_IRI);
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutRightsHolder() {
        jenaModel.getResource(CV_IRI).removeAll(rightsHolder);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithAccrualPeriodicity() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getAccrualPeriodicity()).isEqualTo("IRREG");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutAccrualPeriodicity() {
        jenaModel.getResource(CV_IRI).removeAll(accrualPeriodicity);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithDistribution() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getDistribution()).containsExactlyInAnyOrder("rdf file path",
            "ttl file path");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutDistribution() {
        jenaModel.getResource(CV_IRI).removeAll(distribution);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            PropertyNotFoundException.class);
    }

    @Test
    void shouldExtractMetadataWithSubject() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getSubject()).containsExactlyInAnyOrder("subTheme1", "subTheme2");
    }

    @Test
    void shouldExtractMetadataWithOutSubject() {
        jenaModel.getResource(CV_IRI).removeAll(subject);
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getSubject()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithContactPoint() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getContactPoint()).isEqualTo("Agid");
    }

    @Test
    void shouldExtractMetadataWithOutContactPoint() {
        jenaModel.getResource(CV_IRI).removeAll(contactPoint);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getContactPoint()).isNull();
    }

    @Test
    void shouldExtractMetadataWithPublisher() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getPublisher()).containsExactly("Agid");
    }

    @Test
    void shouldExtractMetadataWithoutPublisher() {
        jenaModel.getResource(CV_IRI).removeAll(publisher);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getPublisher()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithCreator() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getCreator()).containsExactly("stlab");
    }

    @Test
    void shouldExtractMetadataWithoutCreator() {
        jenaModel.getResource(CV_IRI).removeAll(creator);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getCreator()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithVersionInfo() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getVersionInfo()).isEqualTo("1.0");
    }

    @Test
    void shouldExtractMetadataWithoutVersionInfo() {
        jenaModel.getResource(CV_IRI).removeAll(versionInfo);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getVersionInfo()).isNull();
    }

    @Test
    void shouldExtractMetadataWithIssued() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getIssued()).isEqualTo(LocalDate.of(2021, 2, 1));
    }

    @Test
    void shouldExtractMetadataWithOutIssued() {
        jenaModel.getResource(CV_IRI).removeAll(issued);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getIssued()).isNull();
    }

    @Test
    void shouldExtractMetadataWithLanguage() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getLanguage()).containsExactly("ENG");
    }

    @Test
    void shouldExtractMetadataWithoutLanguage() {
        jenaModel.getResource(CV_IRI).removeAll(language);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getLanguage()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithKeywords() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getKeywords()).containsExactlyInAnyOrder("keyword1", "keyword2");
    }

    @Test
    void shouldExtractMetadataWithoutKeywords() {
        jenaModel.getResource(CV_IRI).removeAll(keyword);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getKeywords()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithConformsTo() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getConformsTo()).containsExactly("SKOS");
    }

    @Test
    void shouldExtractMetadataWithoutConformsTo() {
        jenaModel.getResource(CV_IRI).removeAll(conformsTo);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getConformsTo()).isEmpty();
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