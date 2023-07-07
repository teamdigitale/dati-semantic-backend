package it.gov.innovazione.ndc.harvester.model.extractors;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static java.lang.String.format;
import static org.apache.jena.rdf.model.ResourceFactory.createLangLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStringLiteral;
import static org.apache.jena.vocabulary.DCAT.keyword;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiteralExtractorTest {

    @Test
    void shouldExtractLiteralInItalianExcludingNodes() {
        Resource resource = ModelFactory.createDefaultModel().createResource("resourceUri")
            .addProperty(title, createLangLiteral("titel", "de"))
            .addProperty(title, createLangLiteral("titolo", "it"))
            .addProperty(title, createLangLiteral("title", "en"))
            .addProperty(description, createLangLiteral("description", "en"))
            .addProperty(description, createResource("http://example.org/description.png"))
            .addProperty(title, createLangLiteral("शीर्षक", "hi"));

        String extracted = LiteralExtractor.extract(resource, title, NO_VALIDATION);

        assertThat(extracted).isEqualTo("titolo");
    }

    @Test
    void shouldExtractLiteralInEnglish() {
        Resource resource = ModelFactory.createDefaultModel().createResource("resourceUri")
            .addProperty(title, createLangLiteral("titel", "de"))
            .addProperty(title, createLangLiteral("title", "en"))
            .addProperty(description, createLangLiteral("description", "en"))
            .addProperty(title, createLangLiteral("शीर्षक", "hi"));

        String extracted = LiteralExtractor.extract(resource, title, NO_VALIDATION);

        assertThat(extracted).isEqualTo("title");
    }

    @Test
    void shouldExtractLiteralInDefaultLanguage() {
        Resource resource = ModelFactory.createDefaultModel().createResource("resourceUri")
            .addProperty(title, createLangLiteral("titel", "de"))
            .addProperty(description, createLangLiteral("description", "en"))
            .addProperty(title, createStringLiteral("शीर्षक"));

        String extracted = LiteralExtractor.extract(resource, title, NO_VALIDATION);

        assertThat(extracted).isEqualTo("शीर्षक");
    }

    @Test
    void shouldThrowExceptionWhenPropertyNotFound() {
        Resource resource = ModelFactory.createDefaultModel().createResource("resourceUri")
                .addProperty(title, createLangLiteral("titel", "de"))
                .addProperty(title, createStringLiteral("शीर्षक"));

        assertThatThrownBy(() -> LiteralExtractor.extract(resource, description, NO_VALIDATION))
                .isInstanceOf(InvalidModelException.class)
                .hasMessage(
                        format("Cannot find property '%s' for resource '%s'", description, resource));
    }

    @Test
    void shouldValidationDetectExceptionWhenPropertyNotFound() {
        Resource resource = ModelFactory.createDefaultModel().createResource("resourceUri")
                .addProperty(title, createLangLiteral("titel", "de"))
                .addProperty(title, createStringLiteral("शीर्षक"));

        SemanticAssetModelValidationContext validationContext = SemanticAssetModelValidationContext.getForValidation();

        LiteralExtractor.extract(resource, description, validationContext);
        assertThat(validationContext.getErrors()).hasSize(1);
    }

    @Test
    void shouldReturnNullWhenExtractingOptionalProperty() {
        Resource resource = ModelFactory.createDefaultModel().createResource("resourceUri")
                .addProperty(title, createLangLiteral("titel", "de"))
                .addProperty(title, createStringLiteral("शीर्षक"));

        String optional = LiteralExtractor.extractOptional(resource, description, NO_VALIDATION);

        assertThat(optional).isNull();
    }

    @Test
    void shouldExtractMultipleLiterals() {
        Resource resource = ModelFactory.createDefaultModel().createResource("resourceUri")
            .addProperty(keyword, "keyword1")
            .addProperty(keyword, "keyword2")
            .addProperty(title, "title1")
            .addProperty(keyword, "keyword3");

        List<String> multiple = LiteralExtractor.extractAll(resource, keyword);

        assertThat(multiple).containsExactlyInAnyOrder("keyword1", "keyword2", "keyword3");
    }
}
