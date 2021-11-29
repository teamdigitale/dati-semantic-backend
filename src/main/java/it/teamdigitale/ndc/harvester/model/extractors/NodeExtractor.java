package it.teamdigitale.ndc.harvester.model.extractors;

import static java.lang.String.format;

import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.ResourceRequiredException;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PropertyNotFoundException;

public class NodeExtractor {

    public static Resource extractNode(Resource resource, Property property) {
        return extractNodes(resource, property)
            .stream().findFirst()
            .orElseThrow(() -> invalidModelException(resource, property));
    }

    public static Resource extractMaybeNode(Resource resource, Property property) {
        try {
            return extractNode(resource, property);
        } catch (InvalidModelException e) {
            return ResourceFactory.createResource();
        }
    }

    public static List<Resource> extractNodes(Resource resource, Property property) {
        try {
            List<Resource> resources = resource.listProperties(property).toList().stream()
                .map(Statement::getResource)
                .collect(Collectors.toList());
            if (resources.isEmpty()) {
                throw invalidModelException(resource, property);
            }
            return resources;
        } catch (PropertyNotFoundException | ResourceRequiredException e) {
            throw invalidModelException(resource, property);
        }
    }

    public static List<Resource> extractMaybeNodes(Resource resource, Property property) {
        try {
            return extractNodes(resource, property);
        } catch (InvalidModelException e) {
            return List.of();
        }
    }

    private static InvalidModelException invalidModelException(Resource resource,
                                                               Property property) {
        return new InvalidModelException(
            format("Cannot find node '%s' for resource '%s'", property, resource));
    }
}
