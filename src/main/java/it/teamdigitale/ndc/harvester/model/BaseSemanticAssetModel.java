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
import org.apache.jena.rdf.model.Statement;
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
    protected final Model coreModel;
    protected final String source;
    private Resource mainResource;

    public BaseSemanticAssetModel(Model coreModel, String source) {
        this.coreModel = coreModel;
        this.source = source;
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
        List<Resource> resources = coreModel
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
                .theme(mainResource.getRequiredProperty(theme).getString())
                .accrualPeriodicity(mainResource.getRequiredProperty(accrualPeriodicity).getString())
                .distribution(getCollection(mainResource, distribution))
                .subject(getCollection(mainResource, subject))
                .contactPoint(getOptionalProperty(mainResource, contactPoint))
                .publisher(getOptionalProperty(mainResource, publisher))
                .creator(getOptionalProperty(mainResource, creator))
                .versionInfo(getOptionalProperty(mainResource, versionInfo))
                .issued(parseDate(getOptionalProperty(mainResource, issued)))
                .language(getOptionalProperty(mainResource, language))
                .conformsTo(getOptionalProperty(mainResource, conformsTo))
                .build();
    }

    private LocalDate parseDate(String date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return DatatypeConverter.parseDate(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private List<String> getCollection(Resource mainResource, Property property) {
        return mainResource.listProperties(property).toList()
                .stream()
                .map(Statement::getString)
                .collect(Collectors.toList());
    }

    private String getOptionalProperty(Resource resource, Property property) {
        if (resource.hasProperty(property)) {
            return resource.getProperty(property).getString();
        }
        return null;
    }

    private SemanticAssetType getType() {
        return SemanticAssetType.getByIri(getMainResourceTypeIri());
    }

}
