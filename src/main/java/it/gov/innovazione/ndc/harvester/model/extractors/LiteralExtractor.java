package it.gov.innovazione.ndc.harvester.model.extractors;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static it.gov.innovazione.ndc.harvester.model.BaseSemanticAssetModel.maybeThrowInvalidModelException;
import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class LiteralExtractor {

    public static String extractOptional(Resource mainResource, Property versionInfo) {
        return extractOptional(mainResource, versionInfo, NO_VALIDATION);
    }

    public static String extractOptional(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
        try {
            return extract(resource, property, validationContext);
        } catch (InvalidModelException e) {
            validationContext.setWarningValidationType();
            validationContext.addValidationException(e);
            return null;
        }
    }

    public static String extract(Resource mainResource, Property title) {
        return extract(mainResource, title, NO_VALIDATION);
    }

    public static String  extract(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
        List<Statement> properties = resource.listProperties(property).toList();
        Optional<String> stringOptional = properties.stream()
                .filter(s -> s.getObject().isLiteral())
                .filter(filterItalianOrEnglishOrDefault())
                .max((o1, o2) -> o1.getLanguage().compareToIgnoreCase(o2.getLanguage()))
                .map(Statement::getString);

        if (stringOptional.isPresent()) {
            return stringOptional.get();
        }
        maybeThrowInvalidModelException(validationContext, () -> new InvalidModelException(
                format("Cannot find property '%s' for resource '%s'", property, resource)), false);
        return null;
    }

    public static Map<String, String> extractAllLanguages(Resource resource, Property property) {
        Map<String, List<String>> collect = resource.listProperties(property).toList().stream()
                .filter(s -> s.getObject().isLiteral())
                .collect(groupingBy(
                        Statement::getLanguage,
                        mapping(Statement::getString, toList())));

        // log if any entry has more than one value
        collect.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> {
                    String message = format("Found multiple values for language '%s' in property '%s' for resource '%s': %s",
                            e.getKey(), property, resource, e.getValue());
                    log.warn(message);
                });

        return collect
                .entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().sorted().toList().get(0)));
    }

    public static List<String> extractAll(Resource resource, Property property) {
        return resource.listProperties(property).toList().stream()
                .map(LiteralExtractor::extractStringIfPossible)
                .filter(Objects::nonNull)
                .distinct()
                .collect(toList());
    }

    public static List<String> extractAll(List<Resource> resources, Property property) {
        return resources.stream()
                .flatMap(resource -> extractAll(resource, property).stream())
                .distinct()
                .toList();
    }

    private static String extractStringIfPossible(Statement statement) {
        try {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                return statement.getLiteral().getString();
            }
            log.warn("Statement object is not a literal: {}", statement);
            return statement.getObject().toString();
        } catch (Exception e) {
            log.warn("Cannot extract string from statement: {}", statement, e);
            return null;
        }
    }

    private static Predicate<Statement> filterItalianOrEnglishOrDefault() {
        return s -> s.getLanguage().equalsIgnoreCase("it")
                    || s.getLanguage().equalsIgnoreCase("en")
                    || s.getLanguage().isEmpty();
    }

}
