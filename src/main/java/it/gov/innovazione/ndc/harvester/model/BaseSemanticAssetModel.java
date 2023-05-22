package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;

import javax.xml.bind.DatatypeConverter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.contactPoint;
import static org.apache.jena.vocabulary.DCAT.downloadURL;
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

public abstract class BaseSemanticAssetModel implements SemanticAssetModel {
    protected final Model rdfModel;
    protected final String source;
    private Resource mainResource;
    protected final String repoUrl;

    public BaseSemanticAssetModel(Model rdfModel, String source, String repoUrl) {
        this.rdfModel = rdfModel;
        this.source = source;
        this.repoUrl = repoUrl;
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
            throw new InvalidModelException(
                format("No statement for a node whose type is '%s' in '%s'", typeIri, source));
        }
        throw new InvalidModelException(
            format(
                "Found %d statements for nodes whose type is '%s' in '%s', expecting only 1",
                resources.size(), typeIri, source));
    }

    public SemanticAssetMetadata extractMetadata() {
        Resource mainResource = getMainResource();
        return SemanticAssetMetadata.builder()
            .iri(mainResource.getURI())
            .repoUrl(repoUrl)
            .rightsHolder(NodeSummaryExtractor.extractRequiredNodeSummary(mainResource, rightsHolder, FOAF.name))
            .title(LiteralExtractor.extract(mainResource, title))
            .description(LiteralExtractor.extract(mainResource, description))
            .modifiedOn(parseDate(LiteralExtractor.extract(mainResource, modified)))
            .themes(asIriList(NodeExtractor.requireNodes(mainResource, theme)))
            .accrualPeriodicity(NodeExtractor.requireNode(mainResource, accrualPeriodicity).getURI())
            .subjects(asIriList(NodeExtractor.extractMaybeNodes(mainResource, subject)))
            .contactPoint(getContactPoint(mainResource))
            .publishers(NodeSummaryExtractor.maybeNodeSummaries(mainResource, publisher, FOAF.name))
            .creators(NodeSummaryExtractor.maybeNodeSummaries(mainResource, creator, FOAF.name))
            .versionInfo(LiteralExtractor.extractOptional(mainResource, versionInfo))
            .issuedOn(parseDate(LiteralExtractor.extractOptional(mainResource, issued)))
            .languages(asIriList(NodeExtractor.extractMaybeNodes(mainResource, language)))
            .keywords(LiteralExtractor.extractAll(mainResource, keyword))
            .temporal(LiteralExtractor.extractOptional(mainResource, temporal))
            .conformsTo(NodeSummaryExtractor.maybeNodeSummaries(mainResource, conformsTo, FOAF.name))
            .distributions(getDistributions())
            .status(LiteralExtractor.extractAll(mainResource, Admsapit.status))
            .build();
    }

    protected List<Distribution> getDistributions() {
        return emptyList();
    }

    public NodeSummary getContactPoint(Resource mainResource) {
        Resource contactPointNode = NodeExtractor.extractNode(mainResource, contactPoint);
        if (Objects.nonNull(contactPointNode)) {
            Resource email = NodeExtractor.extractNode(contactPointNode, VCARD4.hasEmail);
            if (Objects.nonNull(email)) {
                return NodeSummary.builder()
                    .iri(contactPointNode.getURI())
                    .summary(email.getURI())
                    .build();
            }
        }
        return null;
    }

    public List<String> asIriList(List<Resource> resources) {
        return resources.stream().map(Resource::getURI)
            .collect(Collectors.toList());
    }

    public LocalDate parseDate(String date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return DatatypeConverter.parseDate(date).toInstant().atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    protected Distribution buildDistribution(Resource distNode) {
        String downloadUrl = extractMaybePropertyValue(distNode, downloadURL);
        if (Objects.isNull(downloadUrl)) {
            throw new InvalidModelException(String.format("Invalid distribution '%s': missing %s property",
                    distNode.getURI(), downloadURL));
        }
        String accessUrl = extractMaybePropertyValue(distNode, accessURL);
        return Distribution.builder().accessUrl(accessUrl).downloadUrl(downloadUrl).build();
    }

    private String extractMaybePropertyValue(Resource distNode, Property property) {
        Statement statement = distNode.getProperty(property);
        return Objects.nonNull(statement) ? statement.getResource().getURI() : null;
    }

    protected boolean distributionHasFormat(Resource dist, Property expectedFormatProperty) {
        Statement format = dist.getProperty(DCTerms.format);
        return Objects.nonNull(format) && format.getResource().getURI().equals(expectedFormatProperty.getURI());
    }

    protected List<Distribution> extractDistributionsFilteredByFormat(Property distributionProperty, Property formatPropertyValue) {
        return NodeExtractor.requireNodes(getMainResource(), distributionProperty).stream()
            .filter(dist -> distributionHasFormat(dist, formatPropertyValue))
            .map(this::buildDistribution)
            .collect(Collectors.toList());
    }
}
