package it.teamdigitale.ndc.harvester.model.extractors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import java.util.List;
import java.util.function.Predicate;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

public class LiteralExtractor {

    public static String extractOptional(Resource resource, Property property) {
        try {
            return extract(resource, property);
        } catch (InvalidModelException e) {
            return null;
        }
    }

    public static String extract(Resource resource, Property property) {
        List<Statement> properties = resource.listProperties(property).toList();
        return properties.stream()
            .filter(s -> s.getObject().isLiteral())
            .filter(filterItalianOrEnglishOrDefault())
            .max((o1, o2) -> o1.getLanguage().compareToIgnoreCase(o2.getLanguage()))
            .map(Statement::getString)
            .orElseThrow(() -> new InvalidModelException(
                format("Cannot find property '%s' for resource '%s'", property, resource)));
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
