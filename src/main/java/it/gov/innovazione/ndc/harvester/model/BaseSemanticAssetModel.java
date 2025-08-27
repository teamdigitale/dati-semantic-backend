package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata.Fields;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.VCARD4;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extract;
import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extractAll;
import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor.extractMaybeNodes;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor.requireNode;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor.requireNodes;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor.extractRequiredNodeSummary;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor.maybeNodeSummaries;
import static it.gov.innovazione.ndc.harvester.model.extractors.RightsHolderExtractor.getAgencyId;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
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
    protected final Instance instance;

    @Getter
    protected final SemanticAssetModelValidationContext validationContext;

    protected BaseSemanticAssetModel(Model rdfModel, String source, String repoUrl, Instance instance) {
        this.rdfModel = rdfModel;
        this.source = source;
        this.repoUrl = repoUrl;
        this.validationContext = NO_VALIDATION;
        this.instance = instance;
    }

    protected BaseSemanticAssetModel(Model rdfModel, String source, String repoUrl, SemanticAssetModelValidationContext validationContext, Instance instance) {
        this.rdfModel = rdfModel;
        this.source = source;
        this.repoUrl = repoUrl;
        this.validationContext = validationContext;
        this.instance = instance;
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

        logSemanticInfo(LoggingContext.builder()
                .harvesterStatus(HarvesterRun.Status.RUNNING)
                .repoUrl(repoUrl)
                .message(format("Main resource found: %s", mainResource.getURI()))
                .stage(HarvesterStage.PROCESS_RESOURCE)
                .build());

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

    public static void maybeThrowInvalidModelException(
            SemanticAssetModelValidationContext validationContext,
            Supplier<RuntimeException> exceptionSupplier, boolean throwAnyway) {

        RuntimeException exception = exceptionSupplier.get();
        if (validationContext.getIsValidation()) {
            validationContext.addValidationException(exception);
            if (!throwAnyway) {
                return;
            }
        }
        throw exception;
    }

    private void checkFileDeclaresSingleResource(List<Resource> resources, String typeIri) {
        if (resources.size() == 1) {
            return;
        }

        if (resources.isEmpty()) {
            logSemanticError(LoggingContext.builder()
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .repoUrl(repoUrl)
                    .message(format("No resource of type '%s' in '%s'", typeIri, StringUtils.hasLength(source) ? source : "provided file"))
                    .stage(HarvesterStage.PROCESS_RESOURCE)
                    .additionalInfo("source", source)
                    .additionalInfo("typeIri", typeIri)
                    .build());
            maybeThrowInvalidModelException(validationContext,
                    () -> new InvalidModelException(
                            format("No resource of type '%s' in '%s'", typeIri, StringUtils.hasLength(source) ? source : "provided file")), true);
        }
        logSemanticError(LoggingContext.builder()
                .harvesterStatus(HarvesterRun.Status.RUNNING)
                .repoUrl(repoUrl)
                .message(format("Found %d resources of type '%s' in '%s', expecting only 1",
                        resources.size(), typeIri, StringUtils.hasLength(source) ? source : "provided file"))
                .stage(HarvesterStage.PROCESS_RESOURCE)
                .additionalInfo("source", source)
                .additionalInfo("typeIri", typeIri)
                .additionalInfo("resources", resources.stream().map(Resource::getURI).collect(Collectors.joining(",")))
                .build());
        maybeThrowInvalidModelException(validationContext,
                () -> new InvalidModelException(
                        format("Found %d resources of type '%s' in '%s', expecting only 1",
                                resources.size(), typeIri, StringUtils.hasLength(source) ? source : "provided file")), true);
    }

    private static NodeSummary getContactPoint(Resource mainResource) {
        return getContactPoint(mainResource, NO_VALIDATION);
    }

    private static NodeSummary getContactPoint(Resource mainResource, SemanticAssetModelValidationContext validationContext) {
        Resource contactPointNode = NodeExtractor.extractNode(mainResource, contactPoint, validationContext);
        if (Objects.nonNull(contactPointNode)) {
            Resource email = NodeExtractor.extractNode(contactPointNode, VCARD4.hasEmail, validationContext);
            if (Objects.nonNull(email)) {
                return NodeSummary.builder()
                        .iri(contactPointNode.getURI())
                        .summary(email.getURI())
                        .build();
            }
        }
        return null;
    }

    private static Distribution buildDistribution(Resource distNode) {
        String downloadUrl = extractMaybePropertyValue(distNode, downloadURL);
        if (Objects.isNull(downloadUrl)) {
            throw new InvalidModelException(String.format("Invalid distribution '%s': missing %s property",
                    distNode.getURI(), downloadURL));
        }
        String accessUrl = extractMaybePropertyValue(distNode, accessURL);
        return Distribution.builder().accessUrl(accessUrl).downloadUrl(downloadUrl).build();
    }

    private static String extractMaybePropertyValue(Resource distNode, Property property) {
        Statement statement = distNode.getProperty(property);
        return Objects.nonNull(statement) ? statement.getResource().getURI() : null;
    }

    private static boolean distributionHasFormat(Resource dist, Property expectedFormatProperty) {
        Statement format = dist.getProperty(DCTerms.format);
        return Objects.nonNull(format) && format.getResource().getURI().equals(expectedFormatProperty.getURI());
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

    protected static List<Distribution> extractDistributionsFilteredByFormat(
            Resource mainResource,
            Property distributionProperty,
            Property formatPropertyValue,
            SemanticAssetModelValidationContext validationContext) {
        return requireNodes(mainResource, distributionProperty, validationContext).stream()
                .filter(dist -> distributionHasFormat(dist, formatPropertyValue))
                .map(BaseSemanticAssetModel::buildDistribution)
                .collect(Collectors.toList());
    }

    public SemanticAssetMetadata extractMetadata() {
        Resource mainResource = getMainResource();
        List<Resource> owlClasses = rdfModel.listResourcesWithProperty(RDF.type)
                .toList().stream()
                .filter(BaseSemanticAssetModel::isOwlClass)
                .toList();
        RightsHolder agencyId = getAgencyId(mainResource, validationContext);
        return SemanticAssetMetadata.builder()
                .instance(instance.name())
                .iri(mainResource.getURI())
                .repoUrl(repoUrl)
                .rightsHolder(extractRequiredNodeSummary(mainResource, rightsHolder, FOAF.name))
                .title(extract(mainResource, title))
                .description(extract(mainResource, description))
                .modifiedOn(parseDate(extract(mainResource, modified)))
                .themes(asIriList(requireNodes(mainResource, theme)))
                .accrualPeriodicity(requireNode(mainResource, accrualPeriodicity).getURI())
                .subjects(asIriList(extractMaybeNodes(mainResource, subject)))
                .contactPoint(getContactPoint(mainResource))
                .publishers(maybeNodeSummaries(mainResource, publisher, FOAF.name))
                .creators(maybeNodeSummaries(mainResource, creator, FOAF.name))
                .versionInfo(extractOptional(mainResource, versionInfo))
                .issuedOn(parseDate(extractOptional(mainResource, issued)))
                .languages(asIriList(extractMaybeNodes(mainResource, language)))
                .keywords(extractAll(mainResource, keyword))
                .temporal(extractOptional(mainResource, temporal))
                .conformsTo(maybeNodeSummaries(mainResource, conformsTo, FOAF.name))
                .distributions(getDistributions())
                .status(extractAll(mainResource, Admsapit.status))
                .agencyId(agencyId.getIdentifier())
                .agencyLabel(new ArrayList<>(agencyId.getName().values()))
                .labels(extractAll(owlClasses, RDFS.label))
                .comments(extractAll(owlClasses, RDFS.comment))
                .build();
    }

    private static boolean isOwlClass(Resource resource) {
        return resource.hasProperty(RDF.type, OWL.Class);
    }

    public SemanticAssetModelValidationContext validateMetadata() {
        return Stream.<Consumer<SemanticAssetModelValidationContext>>of(
                        v -> extractRequiredNodeSummary(getMainResource(), rightsHolder, FOAF.name, v.field(Fields.rightsHolder)),
                        v -> extract(getMainResource(), title, v.error().field(Fields.title)),
                        v -> extract(getMainResource(), description, v.error().field(Fields.description)),
                        v -> extract(getMainResource(), modified, v.error().field(Fields.modifiedOn)),
                        v -> requireNodes(getMainResource(), theme, v.error().field(Fields.themes)),
                        v -> requireNode(getMainResource(), accrualPeriodicity, v.error().field(Fields.accrualPeriodicity)),
                        v -> extractMaybeNodes(getMainResource(), subject, v.warning().field(Fields.subjects)),
                        v -> getContactPoint(getMainResource(), v.warning().field(Fields.contactPoint)),
                        v -> maybeNodeSummaries(getMainResource(), publisher, FOAF.name, v.warning().field(Fields.publishers)),
                        v -> maybeNodeSummaries(getMainResource(), creator, FOAF.name, v.warning().field(Fields.creators)),
                        v -> extractOptional(getMainResource(), versionInfo, v.warning().field(Fields.versionInfo)),
                        v -> extractOptional(getMainResource(), issued, v.warning().field(Fields.issuedOn)),
                        v -> extractMaybeNodes(getMainResource(), language, v.warning().field(Fields.languages)),
                        v -> extractOptional(getMainResource(), temporal, v.warning().field(Fields.temporal)),
                        v -> maybeNodeSummaries(getMainResource(), conformsTo, FOAF.name, v.warning().field(Fields.conformsTo)),
                        v -> getDistributions(v.error().field(Fields.distributions)),
                        v -> getAgencyId(getMainResource(), v.error().field(Fields.agencyId))
                )
                .map(consumer -> returningValidationContext(this.validationContext, consumer))
                .reduce(SemanticAssetModelValidationContext::merge)
                .orElse(this.validationContext);
    }

    protected SemanticAssetModelValidationContext returningValidationContext(
            SemanticAssetModelValidationContext validationContext,
            Consumer<SemanticAssetModelValidationContext> validationContextConsumer) {
        try {
            validationContextConsumer.accept(validationContext);
            return validationContext;
        } catch (InvalidModelException e) {
            validationContext.addValidationException(e);
            return validationContext;
        }
    }

    protected List<Distribution> getDistributions() {
        return getDistributions(NO_VALIDATION);
    }

    protected List<Distribution> getDistributions(SemanticAssetModelValidationContext validationContext) {
        return emptyList();
    }
}
