package it.gov.innovazione.ndc.harvester.model;

import com.github.jsonldjava.shaded.com.google.common.collect.ImmutableList;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.extractors.RightsHolderExtractor;
import it.gov.innovazione.ndc.harvester.model.index.Distribution;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.profiles.NDC;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import java.util.List;
import java.util.function.Consumer;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext.NO_VALIDATION;
import static it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
import static java.lang.String.format;
import static org.apache.jena.vocabulary.DCAT.distribution;

@Slf4j
public class ControlledVocabularyModel extends BaseSemanticAssetModel {
    public static final String NDC_ENDPOINT_URL_TEMPLATE = "%s/vocabularies/%s/%s";
    public static final String KEY_CONCEPT_VALIDATION_PATTERN = "^\\w(:?[\\w-]+\\w)*$";

    private String endpointUrl = "";

    public ControlledVocabularyModel(Model coreModel, String source, String repoUrl, Instance instance) {
        super(coreModel, source, repoUrl, instance);
    }

    private ControlledVocabularyModel(Model coreModel, String source, String repoUrl, SemanticAssetModelValidationContext validationContext, Instance instance) {
        super(coreModel, source, repoUrl, validationContext, instance);
    }

    public static ControlledVocabularyModel forValidation(Model coreModel, String source, String repoUrl, Instance instance) {
        return new ControlledVocabularyModel(coreModel, source, repoUrl, SemanticAssetModelValidationContext.getForValidation(), instance);
    }

    public String getKeyConcept() {
        return getKeyConcept(getMainResource(), NO_VALIDATION);
    }

    public static String getKeyConcept(Resource mainResource, SemanticAssetModelValidationContext validationContext) {
        StmtIterator stmtIterator = mainResource.listProperties(NDC.keyConcept);
        String keyConcept;
        try {
            if (!stmtIterator.hasNext()) {
                log.warn("No key concept ({}) statement for controlled vocabulary '{}'",
                        NDC.keyConcept, mainResource);

                InvalidModelException invalidModelException = new InvalidModelException(
                        "No key concept property for controlled vocabulary " + mainResource);

                validationContext.addValidationException(invalidModelException);

                throw invalidModelException;
            }

            Statement statement = stmtIterator.nextStatement();
            if (stmtIterator.hasNext()) {
                log.warn("Multiple key concept ({}) statements for controlled vocabulary '{}'",
                        NDC.keyConcept, mainResource);
                InvalidModelException invalidModelException = new InvalidModelException(
                        "Multiple key concept properties for controlled vocabulary " + mainResource);
                validationContext.addValidationException(invalidModelException);
                throw invalidModelException;
            }

            keyConcept = statement.getObject().toString();
        } finally {
            stmtIterator.close();
        }

        validateKeyConcept(keyConcept, mainResource, validationContext);
        return keyConcept;
    }

    private static void validateKeyConcept(String keyConcept, Resource mainResource, SemanticAssetModelValidationContext validationContext) {
        if (!keyConcept.matches(KEY_CONCEPT_VALIDATION_PATTERN)) {
            log.warn("Key concept string ({}) invalid for controlled vocabulary '{}'",
                    keyConcept, mainResource);
            InvalidModelException invalidModelException = new InvalidModelException(format("Key concept '%s' value does not meet expected pattern", keyConcept));
            validationContext.addValidationException(invalidModelException);
            throw invalidModelException;
        }
    }

    public RightsHolder getAgencyId() {
        return RightsHolderExtractor.getAgencyId(getMainResource(), NO_VALIDATION);
    }

    public void addNdcDataServiceProperties(String baseUrl) {
        endpointUrl = buildEndpointUrl(baseUrl);
        Resource dataServiceNode = rdfModel.createResource(buildDataServiceIndividualUri());
        rdfModel.add(dataServiceNode, RDF.type, NDC.DataService);
        rdfModel.add(dataServiceNode, NDC.servesDataset, getMainResource());
        rdfModel.add(getMainResource(), NDC.hasDataService, dataServiceNode);
        rdfModel.add(dataServiceNode, NDC.endpointURL, endpointUrl);
        logSemanticInfo(LoggingContext.builder()
                .message("Added NDC data service properties")
                .stage(HarvesterStage.PROCESS_RESOURCE)
                .harvesterStatus(HarvesterRun.Status.RUNNING)
                .additionalInfo("dataServiceNode", dataServiceNode)
                .additionalInfo("endpointUrl", endpointUrl)
                .build());
    }


    private String buildDataServiceIndividualUri() {
        return format("https://w3id.org/italia/data/data-service/%s-%s", getAgencyId().getIdentifier(), getKeyConcept());
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    private String buildEndpointUrl(String baseUrl) {
        return format(NDC_ENDPOINT_URL_TEMPLATE, baseUrl, getAgencyId().getIdentifier(), getKeyConcept());
    }

    @Override
    protected String getMainResourceTypeIri() {
        return CONTROLLED_VOCABULARY.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder()
                .type(CONTROLLED_VOCABULARY)
                .distributions(getDistributions())
                .keyConcept(getKeyConcept())
                .endpointUrl(getEndpointUrl())
                .build();
    }

    @Override
    public SemanticAssetModelValidationContext validateMetadata() {
        SemanticAssetModelValidationContext superContext = super.validateMetadata();

        SemanticAssetModelValidationContext context = new ImmutableList.Builder<Consumer<SemanticAssetModelValidationContext>>()
                .add(v -> getDistributions(v.error().field(SemanticAssetMetadata.Fields.distributions)))
                .add(v -> getKeyConcept(getMainResource(), v.warning().field(SemanticAssetMetadata.Fields.keyConcept)))
                .add(v -> RightsHolderExtractor.getAgencyId(getMainResource(), v.warning().field(SemanticAssetMetadata.Fields.agencyId)))
                .build()
                .stream()
                .map(consumer -> returningValidationContext(this.validationContext, consumer))
                .reduce(SemanticAssetModelValidationContext::merge)
                .orElse(superContext);

        return SemanticAssetModelValidationContext.merge(superContext, context);
    }

    @Override
    protected List<Distribution> getDistributions(SemanticAssetModelValidationContext validationContext) {
        return extractDistributionsFilteredByFormat(getMainResource(), distribution, FILE_TYPE_RDF_TURTLE, validationContext);
    }
}
