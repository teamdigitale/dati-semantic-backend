package it.gov.innovazione.ndc.harvester.model.extractors;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.model.BaseSemanticAssetModel.maybeThrowInvalidModelException;
import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor.extractMaybeNodes;
import static java.lang.String.format;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeSummaryExtractor {

    public static NodeSummary extractRequiredNodeSummary(Resource mainResource, Property rightsHolder, Property name) {
        return extractRequiredNodeSummary(mainResource, rightsHolder, name, NO_VALIDATION);
    }

    public static NodeSummary extractRequiredNodeSummary(Resource resource, Property nodeProperty,
                                                         Property summaryProperty, SemanticAssetModelValidationContext validationContext) {
        Optional<NodeSummary> first = maybeNodeSummaries(resource, nodeProperty, summaryProperty, validationContext)
                .stream()
                .findFirst();

        if (first.isPresent()) {
            return first.get();
        }
        maybeThrowInvalidModelException(validationContext, () -> invalidModelException(resource, nodeProperty), false);
        return null;
    }

    public static List<NodeSummary> maybeNodeSummaries(Resource mainResource, Property publisher, Property name) {
        return maybeNodeSummaries(mainResource, publisher, name, NO_VALIDATION);
    }

    public static List<NodeSummary> maybeNodeSummaries(Resource resource, Property nodeProperty,
                                                       Property summaryProperty, SemanticAssetModelValidationContext validationContext) {
        return extractMaybeNodes(resource, nodeProperty, validationContext)
                .stream()
                .map(node -> NodeSummary.builder()
                        .iri(node.getURI())
                        .summary(extractOptional(node, summaryProperty, validationContext))
                        .build())
                .collect(Collectors.toList());
    }

    public static InvalidModelException invalidModelException(Resource resource,
                                                              Property property) {
        return new InvalidModelException(
                format("Unable to extract node summary from resource '%s' using '%s'", resource,
                        property));
    }

}
