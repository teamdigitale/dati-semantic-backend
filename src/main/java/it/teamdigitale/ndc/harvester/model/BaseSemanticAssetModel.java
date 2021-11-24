package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import static java.lang.String.format;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import org.apache.jena.shared.PropertyNotFoundException;
import static org.apache.jena.vocabulary.DCAT.contactPoint;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCAT.theme;
import static org.apache.jena.vocabulary.DCTerms.accrualPeriodicity;
import static org.apache.jena.vocabulary.DCTerms.conformsTo;
import static org.apache.jena.vocabulary.DCTerms.creator;
import static org.apache.jena.vocabulary.DCTerms.description;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.issued;
import static org.apache.jena.vocabulary.DCTerms.language;
import static org.apache.jena.vocabulary.DCTerms.modified;
import static org.apache.jena.vocabulary.DCTerms.publisher;
import static org.apache.jena.vocabulary.DCTerms.rightsHolder;
import static org.apache.jena.vocabulary.DCTerms.subject;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.OWL.versionInfo;
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
                .rightsHolder(
                        mainResource.getRequiredProperty(rightsHolder).getResource().getURI())
                .identifier(mainResource.getRequiredProperty(identifier).getString())
                .type(getType())
                .title(mainResource.getRequiredProperty(title).getString())
                .description(mainResource.getRequiredProperty(description).getString())
                .modified(parseDate(mainResource.getRequiredProperty(modified).getString()))
                .theme(mainResource.getRequiredProperty(theme).getResource().getURI())
                .accrualPeriodicity(mainResource.getRequiredProperty(accrualPeriodicity).getResource().getURI())
                .distribution(getCollection(mainResource, distribution, true))
                .subject(getCollection(mainResource, subject, false))
                .contactPoint(getOptionalResource(mainResource, contactPoint))
                .publisher(getOptionalResource(mainResource, publisher))
                .creator(getOptionalResource(mainResource, creator))
                .versionInfo(getOptionalProperty(mainResource, versionInfo))
                .issued(parseDate(getOptionalProperty(mainResource, issued)))
                .language(getOptionalResource(mainResource, language))
                .conformsTo(getOptionalResource(mainResource, conformsTo))
                .build();
    }

    private String getOptionalProperty(Resource mainResource, Property property) {
        try {
            return mainResource.getRequiredProperty(property).getString();
        } catch (PropertyNotFoundException e) {
            return null;
        }
    }

    private String getOptionalResource(Resource mainResource, Property property) {
        try {
            return mainResource.getRequiredProperty(property).getResource().getURI();
        } catch (PropertyNotFoundException e) {
            return null;
        }
    }

    private LocalDate parseDate(String date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return DatatypeConverter.parseDate(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private List<String> getCollection(Resource mainResource, Property property, boolean isMandatory) {
        List<String> items = mainResource.listProperties(property).toList()
                .stream()
                .map(statement -> statement.getResource().getURI())
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
