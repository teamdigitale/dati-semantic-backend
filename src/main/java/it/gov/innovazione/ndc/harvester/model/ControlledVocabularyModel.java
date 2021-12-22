package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.extractors.NodeExtractor;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.model.profiles.EuropePublicationVocabulary;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.model.profiles.NDC;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static java.lang.String.format;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCTerms.format;

@Slf4j
public class ControlledVocabularyModel extends BaseSemanticAssetModel {
    public static final String NDC_ENDPOINT_URL_TEMPLATE = "%s/vocabularies/%s/%s";
    public static final String KEY_CONCEPT_VALIDATION_PATTERN = "^\\w(:?[\\w-]+\\w)*$";

    private String endpointUrl = "";

    public ControlledVocabularyModel(Model coreModel, String source, String repoUrl) {
        super(coreModel, source, repoUrl);
    }

    public String getKeyConcept() {
        Resource mainResource = getMainResource();
        StmtIterator stmtIterator = mainResource.listProperties(NDC.keyConcept);
        String keyConcept;
        try {
            if (!stmtIterator.hasNext()) {
                log.warn("No key concept ({}) statement for controlled vocabulary '{}'",
                        NDC.keyConcept, mainResource);
                throw new InvalidModelException(
                        "No key concept property for controlled vocabulary " + mainResource);
            }

            Statement statement = stmtIterator.nextStatement();
            if (stmtIterator.hasNext()) {
                log.warn("Multiple key concept ({}) statements for controlled vocabulary '{}'",
                        NDC.keyConcept, mainResource);
                throw new InvalidModelException(
                        "Multiple key concept properties for controlled vocabulary " + mainResource);
            }

            keyConcept = statement.getObject().toString();
        } finally {
            stmtIterator.close();
        }

        validateKeyConcept(keyConcept, mainResource);
        return keyConcept;
    }

    private void validateKeyConcept(String keyConcept, Resource mainResource) {
        if (!keyConcept.matches(KEY_CONCEPT_VALIDATION_PATTERN)) {
            log.warn("Key concept string ({}) invalid for controlled vocabulary '{}'",
                    keyConcept, mainResource);
            throw new InvalidModelException(format("Key concept '%s' value does not meet expected pattern", keyConcept));
        }
    }

    public String getAgencyId() {
        Statement rightsHolder;
        try {
            rightsHolder = getMainResource().getRequiredProperty(DCTerms.rightsHolder);
        } catch (Exception e) {
            throw new InvalidModelException(format("Cannot find required rightsHolder property (%s)", DCTerms.rightsHolder));
        }
        Statement idProperty;
        try {
            idProperty = rightsHolder.getProperty(DCTerms.identifier);
        } catch (Exception e) {
            String rightsHolderIri = rightsHolder.getObject().toString();
            throw new InvalidModelException(format("Cannot find required id (%s) for rightsHolder '%s'", DCTerms.identifier, rightsHolderIri));
        }
        return idProperty.getString();
    }

    public void addNdcDataServiceProperties(String baseUrl) {
        endpointUrl = buildEndpointUrl(baseUrl);
        Resource dataServiceNode = rdfModel.createResource(buildDataServiceIndividualUri());
        rdfModel.add(dataServiceNode, RDF.type, NDC.DataService);
        rdfModel.add(dataServiceNode, NDC.servesDataset, getMainResource());
        rdfModel.add(getMainResource(), NDC.hasDataService, dataServiceNode);
        rdfModel.add(dataServiceNode, NDC.endpointURL, endpointUrl);
    }

    private String buildDataServiceIndividualUri() {
        return format("https://w3id.org/italia/data/data-service/%s-%s", getAgencyId(), getKeyConcept());
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    private String buildEndpointUrl(String baseUrl) {
        return format(NDC_ENDPOINT_URL_TEMPLATE, baseUrl, getAgencyId(), getKeyConcept());
    }

    @Override
    protected String getMainResourceTypeIri() {
        return CONTROLLED_VOCABULARY.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder()
                .type(CONTROLLED_VOCABULARY)
                .distributionUrls(getDistributionUrls())
                .keyConcept(getKeyConcept())
                .agencyId(getAgencyId())
                .endpointUrl(getEndpointUrl())
                .build();
    }

    private List<String> getDistributionUrls() {
        return NodeExtractor.extractNodes(getMainResource(), distribution).stream()
                .filter(this::isTurtleDistribution)
                .map(node -> requireAccessUrl(node))
                .collect(Collectors.toList());
    }

    private String requireAccessUrl(Resource node) {
        Statement accessUrlProperty = node.getProperty(accessURL);
        if (accessUrlProperty == null) {
            throw new InvalidModelException(String.format("Invalid turtle distribution '%s': missing %s", node.getURI(), accessURL));
        }
        return accessUrlProperty.getResource().getURI();
    }

    private boolean isTurtleDistribution(Resource node) {
        Statement formatProperty = node.getProperty(format);
        if (Objects.isNull(formatProperty)) {
            return false;
        }
        return formatProperty.getResource().getURI().equals(EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE.getURI());
    }

}
