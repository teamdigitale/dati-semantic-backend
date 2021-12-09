package it.teamdigitale.ndc.harvester.model.extractors;

import static org.apache.jena.vocabulary.DCTerms.publisher;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.junit.jupiter.api.Test;

class NodeSummaryExtractorTest {

    @Test
    void mustExtractNodeSummary() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(rightsHolder, defaultModel.createResource("http://rightsHolderUri")
                .addProperty(FOAF.name, "rightsHolderName"));


        NodeSummary nodeSummary =
            NodeSummaryExtractor.extractRequiredNodeSummary(resource, rightsHolder, FOAF.name);

        assertThat(nodeSummary.getSummary()).isEqualTo("rightsHolderName");
        assertThat(nodeSummary.getIri()).isEqualTo("http://rightsHolderUri");
    }

    @Test
    void mustThrowErrorWhenUnableToExtractSummary() {
        Model defaultModel = ModelFactory.createDefaultModel();
        Resource resource = defaultModel.createResource("resourceUri")
            .addProperty(rightsHolder, defaultModel.createResource("http://rightsHolderUri")
                .addProperty(FOAF.name, "rightsHolderName"));


        assertThatThrownBy(
            () -> NodeSummaryExtractor.extractRequiredNodeSummary(resource, publisher, FOAF.name))
            .isInstanceOf(InvalidModelException.class);
    }
}