package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel.KEY_CONCEPT_IRI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createLangLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.contactPoint;
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
import static org.apache.jena.vocabulary.DCTerms.temporal;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.OWL.versionInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.time.LocalDate;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
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
        jenaModel
            .createResource(CV_IRI)
            .addProperty(RDF.type,
                createResource(CONTROLLED_VOCABULARY.getTypeIri()))
            .addProperty(createProperty(KEY_CONCEPT_IRI), "test-concept")
            .addProperty(rightsHolder, jenaModel.createResource(RIGHTS_HOLDER_IRI)
                .addProperty(identifier, "agid")
                .addProperty(FOAF.name, "AgID"))
            .addProperty(title, createLangLiteral("title", "en"))
            .addProperty(description, "description")
            .addProperty(modified, "2021-03-02")
            .addProperty(theme, createResource("theme"))
            .addProperty(accrualPeriodicity, createResource("IRREG"))
            .addProperty(subject, createResource("subTheme1"))
            .addProperty(subject, createResource("subTheme2"))
            .addProperty(contactPoint, jenaModel.createResource("http://contactPoint")
                .addProperty(VCARD4.hasEmail, jenaModel.createResource("mailto:test@test.com")))
            .addProperty(publisher, jenaModel.createResource("http://publisher")
                .addProperty(FOAF.name, "publisher"))
            .addProperty(publisher, jenaModel.createResource("http://publisher2")
                .addProperty(FOAF.name, "publisher2"))
            .addProperty(creator, jenaModel.createResource("http://creator")
                .addProperty(FOAF.name, "creator"))
            .addProperty(creator, jenaModel.createResource("http://creator2")
                .addProperty(FOAF.name, "creator2"))
            .addProperty(versionInfo, "1.0")
            .addProperty(issued, "2021-02-01")
            .addProperty(language, jenaModel.createResource("ENG"))
            .addProperty(keyword, "keyword1").addProperty(keyword, "keyword2")
            .addProperty(temporal, "temporal")
            .addProperty(conformsTo, jenaModel.createResource("http://conformsTo")
                .addProperty(FOAF.name, "conformsTo"))
            .addProperty(conformsTo, jenaModel.createResource("http://conformsTo2")
                .addProperty(FOAF.name, "conformsTo2"));
        semanticAssetModel = new TestBaseSemanticAssetModel(jenaModel, TTL_FILE, "some-repo");
    }

    @Test
    void shouldExtractMetadataWithIri() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getIri()).isEqualTo(CV_IRI);
    }

    @Test
    void shouldExtractMetadataWithSemanticAssetType() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getType()).isEqualTo(CONTROLLED_VOCABULARY);
    }

    @Test
    void shouldExtractWithRepoUrl() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getRepoUrl()).isEqualTo("some-repo");
    }

    @Test
    void shouldExtractMetadataWithTitleInEnglish() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getTitle()).isEqualTo("title");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutTitle() {
        jenaModel.getResource(CV_IRI).removeAll(title);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            InvalidModelException.class);
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
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithModified() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getModifiedOn()).isEqualTo(LocalDate.of(2021, 3, 2));
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutModified() {
        jenaModel.getResource(CV_IRI).removeAll(modified);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithTheme() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getThemes()).containsExactly("theme");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutTheme() {
        jenaModel.getResource(CV_IRI).removeAll(theme);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithRightsHolder() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getRightsHolder().getIri()).isEqualTo(RIGHTS_HOLDER_IRI);
        assertThat(metadata.getRightsHolder().getSummary()).isEqualTo("AgID");
    }

    @Test
    void shouldFailWhenExtractingMetadataWithOutRightsHolder() {
        jenaModel.getResource(CV_IRI).removeAll(rightsHolder);

        assertThatThrownBy(() -> semanticAssetModel.extractMetadata()).isInstanceOf(
            InvalidModelException.class);
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
            InvalidModelException.class);
    }

    @Test
    void shouldExtractMetadataWithSubject() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getSubjects()).containsExactlyInAnyOrder("subTheme1", "subTheme2");
    }

    @Test
    void shouldExtractMetadataWithOutSubject() {
        jenaModel.getResource(CV_IRI).removeAll(subject);
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getSubjects()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithContactPoint() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getContactPoint().getIri()).isEqualTo("http://contactPoint");
        assertThat(metadata.getContactPoint().getSummary()).isEqualTo("mailto:test@test.com");
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

        assertThat(metadata.getPublishers()).hasSize(2);
        assertThat(metadata.getPublishers()).containsExactlyInAnyOrder(
            NodeSummary.builder().iri("http://publisher").summary("publisher").build(),
            NodeSummary.builder().iri("http://publisher2").summary("publisher2").build()
        );
    }

    @Test
    void shouldExtractMetadataWithoutPublisher() {
        jenaModel.getResource(CV_IRI).removeAll(publisher);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getPublishers()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithCreator() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getCreators()).hasSize(2);
        assertThat(metadata.getCreators()).containsExactlyInAnyOrder(
            NodeSummary.builder().iri("http://creator").summary("creator").build(),
            NodeSummary.builder().iri("http://creator2").summary("creator2").build()
        );
    }

    @Test
    void shouldExtractMetadataWithoutCreator() {
        jenaModel.getResource(CV_IRI).removeAll(creator);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getCreators()).isEmpty();
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

        assertThat(metadata.getIssuedOn()).isEqualTo(LocalDate.of(2021, 2, 1));
    }

    @Test
    void shouldExtractMetadataWithOutIssued() {
        jenaModel.getResource(CV_IRI).removeAll(issued);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getIssuedOn()).isNull();
    }

    @Test
    void shouldExtractMetadataWithLanguage() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getLanguages()).containsExactly("ENG");
    }

    @Test
    void shouldExtractMetadataWithoutLanguage() {
        jenaModel.getResource(CV_IRI).removeAll(language);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getLanguages()).isEmpty();
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

        assertThat(metadata.getConformsTo()).hasSize(2);
        assertThat(metadata.getConformsTo()).containsExactlyInAnyOrder(
            NodeSummary.builder().iri("http://conformsTo").summary("conformsTo").build(),
            NodeSummary.builder().iri("http://conformsTo2").summary("conformsTo2").build()
        );
    }

    @Test
    void shouldExtractMetadataWithoutConformsTo() {
        jenaModel.getResource(CV_IRI).removeAll(conformsTo);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getConformsTo()).isEmpty();
    }

    @Test
    void shouldExtractMetadataWithTemporal() {
        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getTemporal()).isEqualTo("temporal");
    }

    @Test
    void shouldExtractMetadataWithoutTemporal() {
        jenaModel.getResource(CV_IRI).removeAll(temporal);

        SemanticAssetMetadata metadata = semanticAssetModel.extractMetadata();

        assertThat(metadata.getTemporal()).isNull();
    }

    private static class TestBaseSemanticAssetModel extends BaseSemanticAssetModel {

        public TestBaseSemanticAssetModel(Model coreModel, String source, String repoUrl) {
            super(coreModel, source, repoUrl);
        }

        @Override
        protected String getMainResourceTypeIri() {
            return CONTROLLED_VOCABULARY.getTypeIri();
        }
    }
}