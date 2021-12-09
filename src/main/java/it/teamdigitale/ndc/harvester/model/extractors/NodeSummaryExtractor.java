package it.teamdigitale.ndc.harvester.model.extractors;

import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractMaybeNodes;
import static java.lang.String.format;

import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class NodeSummaryExtractor {

    public static NodeSummary extractRequiredNodeSummary(Resource resource, Property nodeProperty,
                                                         Property summaryProperty) {
        return maybeNodeSummaries(resource, nodeProperty, summaryProperty)
            .stream()
            .findFirst()
            .orElseThrow(() -> invalidModelException(resource, nodeProperty));
    }

    public static List<NodeSummary> maybeNodeSummaries(Resource resource, Property nodeProperty,
                                                       Property summaryProperty) {
        return extractMaybeNodes(resource, nodeProperty)
            .stream()
            .map(node -> NodeSummary.builder()
                .iri(node.getURI())
                .summary(extractOptional(node, summaryProperty))
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
