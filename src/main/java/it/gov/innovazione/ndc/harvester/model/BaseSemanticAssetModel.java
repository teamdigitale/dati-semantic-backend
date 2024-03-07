package it.gov.innovazione.ndc.harvester.model;

import com.github.jsonldjava.shaded.com.google.common.collect.ImmutableList;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor;
import it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.NodeSummary;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.Admsapit;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
import org.springframework.util.StringUtils;

import javax.xml.bind.DatatypeConverter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extract;
import static it.gov.innovazione.ndc.harvester.model.extractors.LiteralExtractor.extractOptional;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor.extractMaybeNodes;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor.requireNode;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor.requireNodes;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor.extractRequiredNodeSummary;
import static it.gov.innovazione.ndc.harvester.model.extractors.NodeSummaryExtractor.maybeNodeSummaries;
import static it.gov.innovazione.ndc.harvester.model.extractors.RightsHolderExtractor.getAgencyId;
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

    @Getter
    protected final SemanticAssetModelValidationContext validationContext;

    protected BaseSemanticAssetModel(Model rdfModel, String source, String repoUrl) {
        this.rdfModel = rdfModel;
        this.source = source;
        this.repoUrl = repoUrl;
        this.validationContext = NO_VALIDATION;
    }

    protected BaseSemanticAssetModel(Model rdfModel, String source, String repoUrl, SemanticAssetModelValidationContext validationContext) {
        this.rdfModel = rdfModel;
        this.source = source;
        this.repoUrl = repoUrl;
        this.validationContext = validationContext;
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
            maybeThrowInvalidModelException(validationContext,
                    () -> new InvalidModelException(
                            format("No resource of type '%s' in '%s'", typeIri, StringUtils.hasLength(source) ? source : "provided file")), true);
        }
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
        RightsHolder agencyId = getAgencyId(mainResource, validationContext);
        return SemanticAssetMetadata.builder()
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
                .keywords(LiteralExtractor.extractAll(mainResource, keyword))
                .temporal(extractOptional(mainResource, temporal))
                .conformsTo(maybeNodeSummaries(mainResource, conformsTo, FOAF.name))
                .distributions(getDistributions())
                .status(LiteralExtractor.extractAll(mainResource, Admsapit.status))
                .agencyId(agencyId.getIdentifier())
                .agencyLabel(new ArrayList<>(agencyId.getName().values()))
                .build();
    }

    public SemanticAssetModelValidationContext validateMetadata() {
        return new ImmutableList.Builder<Consumer<SemanticAssetModelValidationContext>>()
                .add(v -> extractRequiredNodeSummary(getMainResource(), rightsHolder, FOAF.name, v.withFieldName(
                        SemanticAssetMetadata.Fields.rightsHolder)))
                .add(v -> extract(getMainResource(), title, v.withFieldName(SemanticAssetMetadata.Fields.title)))
                .add(v -> extract(getMainResource(), description, v.withFieldName(SemanticAssetMetadata.Fields.description)))
                .add(v -> extract(getMainResource(), modified, v.withFieldName(SemanticAssetMetadata.Fields.modifiedOn)))
                .add(v -> requireNodes(getMainResource(), theme, v.withFieldName(SemanticAssetMetadata.Fields.themes)))
                .add(v -> requireNode(getMainResource(), accrualPeriodicity, v.withFieldName(SemanticAssetMetadata.Fields.accrualPeriodicity)))
                .add(v -> extractMaybeNodes(getMainResource(), subject, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.subjects)))
                .add(v -> getContactPoint(getMainResource(), v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.contactPoint)))
                .add(v -> maybeNodeSummaries(getMainResource(), publisher, FOAF.name, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.publishers)))
                .add(v -> maybeNodeSummaries(getMainResource(), creator, FOAF.name, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.creators)))
                .add(v -> extractOptional(getMainResource(), versionInfo, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.versionInfo)))
                .add(v -> extractOptional(getMainResource(), issued, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.issuedOn)))
                .add(v -> extractMaybeNodes(getMainResource(), language, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.languages)))
                .add(v -> extractOptional(getMainResource(), temporal, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.temporal)))
                .add(v -> maybeNodeSummaries(getMainResource(), conformsTo, FOAF.name, v.withWarningValidationType().withFieldName(SemanticAssetMetadata.Fields.conformsTo)))
                .add(v -> getDistributions(v.withFieldName(SemanticAssetMetadata.Fields.distributions)))
                .add(v -> getAgencyId(getMainResource(), v.withFieldName(SemanticAssetMetadata.Fields.agencyId)))
                .build()
                .stream()
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
