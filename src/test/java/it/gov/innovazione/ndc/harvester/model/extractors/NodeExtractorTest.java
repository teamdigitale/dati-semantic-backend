package it.gov.innovazione.ndc.harvester.model.extractors;

import static org.apache.jena.rdf.model.ResourceFactory.createLangLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCTerms.creator;
import static org.apache.jena.vocabulary.DCTerms.publisher;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.junit.jupiter.api.Test;

class NodeExtractorTest {

    @Test
    void shouldExtractResource() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(title, createLangLiteral("title", "en"))
            .addProperty(rightsHolder, defaultModel.createResource("http://rightsHolderUri")
                .addProperty(FOAF.name, "rightsHolderName"));

        Resource extractNode = NodeExtractor.extractNode(resource, rightsHolder);

        assertThat(extractNode.getURI()).isEqualTo("http://rightsHolderUri");
        assertThat(extractNode.getProperty(FOAF.name).getString()).isEqualTo("rightsHolderName");
    }

    @Test
    void shouldThrowExceptionWhenResourceWithPropertyNotFound() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(title, createLangLiteral("title", "en"));

        assertThatThrownBy(() -> NodeExtractor.extractNode(resource, rightsHolder))
            .isInstanceOf(InvalidModelException.class)
            .hasMessage(
                String.format("Cannot find node '%s' for resource '%s'", rightsHolder,
                    resource));
    }

    @Test
    void shouldThrowExceptionWhenPropertyIsLiteral() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(title, createLangLiteral("title", "en"));

        assertThatThrownBy(() -> NodeExtractor.extractNode(resource, title))
            .isInstanceOf(InvalidModelException.class)
            .hasMessage(
                String.format("Cannot find node '%s' for resource '%s'", title, resource));
    }

    @Test
    void shouldReturnNullWhenPropertyIsLiteral() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(title, createLangLiteral("title", "en"));

        Resource node = NodeExtractor.extractMaybeNode(resource, title);

        assertThat(node).isNull();
    }

    @Test
    void shouldReturnNullWhenPropertyIsNotFound() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(title, createLangLiteral("title", "en"));

        Resource node = NodeExtractor.extractMaybeNode(resource, rightsHolder);

        assertThat(node).isNull();
    }

    @Test
    void shouldReturnEmptyListWhenPropertyIsNotFound() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(creator, createResource("some-uri"))
            .addProperty(creator, createResource("some-other-uri"));

        List<Resource> resources = NodeExtractor.extractMaybeNodes(resource, publisher);

        assertThat(resources).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenPropertyIsNotNode() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(creator, createResource("some-uri"))
            .addProperty(title, "some-title")
            .addProperty(creator, createResource("some-other-uri"));

        List<Resource> resources = NodeExtractor.extractMaybeNodes(resource, title);

        assertThat(resources).isEmpty();
    }

}