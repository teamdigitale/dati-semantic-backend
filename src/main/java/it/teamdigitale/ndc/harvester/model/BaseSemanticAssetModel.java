package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extract;
import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extractAll;
import static it.teamdigitale.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractMaybeNodes;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNode;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNodes;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeSummaryExtractor.maybeNodeSummaries;
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
import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import it.teamdigitale.ndc.harvester.model.extractors.NodeSummaryExtractor;
import it.teamdigitale.ndc.harvester.model.index.NodeSummary;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;

public abstract class BaseSemanticAssetModel implements SemanticAssetModel {
    protected final Model rdfModel;
    protected final String source;
    private Resource mainResource;
    private String repoUrl;

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
            .rightsHolder(
                NodeSummaryExtractor.mustExtractNodeSummary(mainResource, rightsHolder, FOAF.name))
            .type(getType())
            .title(extract(mainResource, title))
            .description(extract(mainResource, description))
            .modified(parseDate(extract(mainResource, modified)))
            .theme(asIriList(extractNodes(mainResource, theme)))
            .accrualPeriodicity(extractNode(mainResource, accrualPeriodicity).getURI())
            .distribution(asIriList(extractNodes(mainResource, distribution)))
            .subject(asIriList(extractMaybeNodes(mainResource, subject)))
            .contactPoint(getContactPoint(mainResource))
            .publisher(maybeNodeSummaries(mainResource, publisher, FOAF.name))
            .creator(maybeNodeSummaries(mainResource, creator, FOAF.name))
            .versionInfo(extractOptional(mainResource, versionInfo))
            .issued(parseDate(extractOptional(mainResource, issued)))
            .language(asIriList(extractMaybeNodes(mainResource, language)))
            .keywords(extractAll(mainResource, keyword))
            .temporal(extractOptional(mainResource, temporal))
            .conformsTo(maybeNodeSummaries(mainResource, conformsTo, FOAF.name))
            .build();
    }

    private NodeSummary getContactPoint(Resource mainResource) {
        return maybeNodeSummaries(mainResource, contactPoint, VCARD4.hasEmail).stream()
            .findFirst().orElse(null);
    }

    private List<String> asIriList(List<Resource> resources) {
        return resources.stream().map(Resource::getURI)
            .collect(Collectors.toList());
    }

    private LocalDate parseDate(String date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return DatatypeConverter.parseDate(date).toInstant().atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    private SemanticAssetType getType() {
        return SemanticAssetType.getByIri(getMainResourceTypeIri());
    }
}
