package it.gov.innovazione.ndc.harvester.model.extractors;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceRequiredException;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PropertyNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.model.BaseSemanticAssetModel.maybeThrowInvalidModelException;
import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static java.lang.String.format;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeExtractor {


    public static Resource requireNode(Resource mainResource, Property accrualPeriodicity) {
        return requireNode(mainResource, accrualPeriodicity, NO_VALIDATION);
    }

    public static Resource requireNode(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
        try {
            return extractMaybeNode(resource, property, validationContext)
                    .orElseThrow(() -> invalidModelException(resource, property));
        } catch (InvalidModelException e) {
            maybeThrowInvalidModelException(validationContext, () -> e, false);
            return null;
        }
    }

    public static Resource extractNode(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
        return extractMaybeNode(resource, property, validationContext).orElse(null);
    }

    public static List<Resource> requireNodes(Resource mainResource, Property theme) {
        return requireNodes(mainResource, theme, NO_VALIDATION);
    }

    public static List<Resource> requireNodes(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
        try {
            List<Resource> resources = resource.listProperties(property).toList().stream()
                    .map(Statement::getResource)
                    .collect(Collectors.toList());
            if (resources.isEmpty()) {
                maybeThrowInvalidModelException(validationContext, () -> invalidModelException(resource, property), false);
                return List.of();
            }
            return resources;
        } catch (PropertyNotFoundException | ResourceRequiredException e) {
            maybeThrowInvalidModelException(validationContext, () -> invalidModelException(resource, property), false);
            return List.of();
        }
    }

    public static List<Resource> extractMaybeNodes(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
        try {
            return requireNodes(resource, property, validationContext);
        } catch (InvalidModelException e) {
            validationContext.addValidationException(e);
            return List.of();
        }
    }

    public static List<Resource> extractMaybeNodes(Resource mainResource, Property subject) {
        return extractMaybeNodes(mainResource, subject, NO_VALIDATION);
    }

    private static Optional<Resource> extractMaybeNode(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
        return extractMaybeNodes(resource, property, validationContext)
                .stream().findFirst();
    }

    public static InvalidModelException invalidModelException(Resource resource,
                                                              Property property) {
        return new InvalidModelException(
                format("Cannot find node '%s' for resource '%s'", property, resource));
    }

}
