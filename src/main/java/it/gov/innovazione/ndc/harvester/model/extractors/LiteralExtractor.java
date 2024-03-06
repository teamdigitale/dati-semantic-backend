package it.gov.innovazione.ndc.harvester.model.extractors;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static it.gov.innovazione.ndc.harvester.model.BaseSemanticAssetModel.maybeThrowInvalidModelException;
import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    public static String extract(Resource resource, Property property, SemanticAssetModelValidationContext validationContext) {
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
        return resource.listProperties(property).toList().stream()
                .filter(s -> s.getObject().isLiteral())
                .map(s -> Pair.of(s.getLanguage(), s.getString()))
                .collect(toMap(Pair::getLeft, Pair::getRight));
    }

    public static List<String> extractAll(Resource resource, Property property) {
        return resource.listProperties(property).toList().stream()
                .map(Statement::getString)
                .collect(toList());
    }

    private static Predicate<Statement> filterItalianOrEnglishOrDefault() {
        return s -> s.getLanguage().equalsIgnoreCase("it")
                    || s.getLanguage().equalsIgnoreCase("en")
                    || s.getLanguage().isEmpty();
    }

}
