package it.teamdigitale.ndc.harvester.model;

import static java.lang.String.format;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.contactPoint;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.keyword;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.accrualPeriodicity;
import static org.apache.jena.vocabulary.DCTerms.conformsTo;
import static org.apache.jena.vocabulary.DCTerms.creator;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.issued;
import static org.apache.jena.vocabulary.DCTerms.language;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.publisher;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.subject;
import static org.apache.jena.vocabulary.DCTerms.temporal;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.OWL.versionInfo;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.vocabulary.RDF;

public abstract class BaseSemanticAssetModel implements SemanticAssetModel {
    protected final Model rdfModel;
    protected final String source;
    private Resource mainResource;

    public BaseSemanticAssetModel(Model rdfModel, String source) {
        this.rdfModel = rdfModel;
        this.source = source;
    }

    @Override
    public Model getRdfModel() {
        return rdfModel;
    }

    @Override
    public Resource getMainResource() {
        if (mainResource == null) {
            mainResource = getUniqueResourceByType(getMainResourceTypeIri());
        }

        return mainResource;
    }

    protected abstract String getMainResourceTypeIri();

    private Resource getUniqueResourceByType(String resourceTypeIri) {
        List<Resource> resources = rdfModel
            .listResourcesWithProperty(RDF.type, createResource(resourceTypeIri))
            .toList();

        checkFileDeclaresSingleResource(resources, resourceTypeIri);
        return resources.get(0);
    }

    private void checkFileDeclaresSingleResource(List<Resource> resources, String typeIri) {
        if (resources.size() == 1) {
            return;
        }

        if (resources.isEmpty()) {
            throw new InvalidAssetException(
                format("No statement for a node whose type is '%s' in '%s'", typeIri, source));
        }
        throw new InvalidAssetException(
            format(
                "Found %d statements for nodes whose type is '%s' in '%s', expecting only 1",
                resources.size(), typeIri, source));
    }

    public SemanticAssetMetadata extractMetadata() {
        Resource mainResource = getMainResource();
        return SemanticAssetMetadata.builder()
            .iri(mainResource.getURI())
            .rightsHolder(getRequiredProperty(rightsHolder, resourceMapper()))
            .type(getType())
            .title(getItalianOrEnglishOrDefaultValue(title, true))
            .description(getItalianOrEnglishOrDefaultValue(description, true))
            .modified(parseDate(getRequiredProperty(modified, propertyMapper())))
            .theme(getCollection(theme, true, resourceMapper()))
            .accrualPeriodicity(getRequiredProperty(accrualPeriodicity, resourceMapper()))
            .distribution(getCollection(distribution, true, resourceMapper()))
            .subject(getCollection(subject, false, resourceMapper()))
            .contactPoint(getOptional(contactPoint, resourceMapper()))
            .publisher(getCollection(publisher, false, resourceMapper()))
            .creator(getCollection(creator, false, resourceMapper()))
            .versionInfo(getItalianOrEnglishOrDefaultValue(versionInfo, false))
            .issued(parseDate(getOptional(issued, propertyMapper())))
            .language(getCollection(language, false, resourceMapper()))
            .keywords(getCollection(keyword, false, propertyMapper()))
            .temporal(getOptional(temporal, propertyMapper()))
            .conformsTo(getCollection(conformsTo, false, resourceMapper()))
            .build();
    }

    private String getRequiredProperty(Property property, Function<Statement, String> mapper) {
        return Optional.ofNullable(getMainResource().getProperty(property))
            .map(mapper)
            .orElseThrow(() -> new PropertyNotFoundException(property));
    }

    private String getItalianOrEnglishOrDefaultValue(Property property, boolean isRequired) {
        List<Statement> properties = getMainResource().listProperties(property).toList();
        return properties.stream()
            .filter(filterItalianOrEnglishOrDefault())
            .max((o1, o2) -> o1.getLanguage().compareToIgnoreCase(o2.getLanguage()))
            .map(Statement::getString)
            .or(() -> {
                if (isRequired) {
                    throw new PropertyNotFoundException(property);
                }
                return Optional.empty();
            }).orElse(null);
    }

    private Predicate<Statement> filterItalianOrEnglishOrDefault() {
        return s -> s.getLanguage().equalsIgnoreCase("it")
            || s.getLanguage().equalsIgnoreCase("en")
            || s.getLanguage().isEmpty();
    }

    private Function<Statement, String> resourceMapper() {
        return statement -> statement.getResource().getURI();
    }

    private Function<Statement, String> propertyMapper() {
        return Statement::getString;
    }

    private String getOptional(Property property, Function<Statement, String> mapper) {
        return Optional.ofNullable(getMainResource().getProperty(property))
            .map(mapper)
            .orElse(null);
    }

    private LocalDate parseDate(String date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return DatatypeConverter.parseDate(date).toInstant().atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    private List<String> getCollection(Property property, boolean isMandatory,
                                       Function<Statement, String> mapper) {
        List<String> items = getMainResource().listProperties(property).toList()
            .stream()
            .map(mapper)
            .collect(Collectors.toList());
        if (items.isEmpty() && isMandatory) {
            throw new PropertyNotFoundException(property);
        }
        return items;
    }

    private SemanticAssetType getType() {
        return SemanticAssetType.getByIri(getMainResourceTypeIri());
    }

}
