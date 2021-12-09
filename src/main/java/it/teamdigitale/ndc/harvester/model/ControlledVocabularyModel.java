package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.model.NDC;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNodes;
import static it.teamdigitale.ndc.harvester.model.vocabulary.EuropePublicationVocabulary.FILE_TYPE_RDF_TURTLE;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.vocabulary.DCAT.accessURL;
import static org.apache.jena.vocabulary.DCAT.distribution;
import static org.apache.jena.vocabulary.DCTerms.format;

@Slf4j
public class ControlledVocabularyModel extends BaseSemanticAssetModel {
    public static final String NDC_PREFIX = "https://w3id.org/italia/onto/ndc-profile/";
    public static final String KEY_CONCEPT_IRI = NDC_PREFIX + "keyConcept";
    public static final String NDC_ENDPOINT_URL_TEMPLATE = "%s/vocabularies/%s/%s";

    private String endpointUrl = "";

    public ControlledVocabularyModel(Model coreModel, String source, String repoUrl) {
        super(coreModel, source, repoUrl);
    }

    public String getKeyConcept() {
        Property keyConceptProperty = createProperty(KEY_CONCEPT_IRI);
        Resource mainResource = getMainResource();
        StmtIterator stmtIterator = mainResource.listProperties(keyConceptProperty);
        try {
            if (!stmtIterator.hasNext()) {
                log.warn("No key concept ({}) statement for controlled vocabulary '{}'",
                        KEY_CONCEPT_IRI, mainResource);
                throw new InvalidModelException(
                        "No key concept property for controlled vocabulary " + mainResource);
            }

            Statement statement = stmtIterator.nextStatement();
            if (stmtIterator.hasNext()) {
                log.warn("Multiple key concept ({}) statements for controlled vocabulary '{}'",
                        KEY_CONCEPT_IRI, mainResource);
                throw new InvalidModelException(
                        "Multiple key concept properties for controlled vocabulary " + mainResource);
            }

            return statement.getObject().toString();
        } finally {
            stmtIterator.close();
        }
    }

    public String getAgencyId() {
        return getMainResource()
                .getRequiredProperty(DCTerms.rightsHolder)
                .getProperty(DCTerms.identifier)
                .getString();
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
        return getMainResource().getURI() + "/DataService";
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    private String buildEndpointUrl(String baseUrl) {
        return String.format(NDC_ENDPOINT_URL_TEMPLATE, baseUrl, getAgencyId(), getKeyConcept());
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
        return extractNodes(getMainResource(), distribution).stream()
            .filter(node -> Objects.nonNull(node.getProperty(format))
                    && node.getProperty(format).getResource().getURI().equals(FILE_TYPE_RDF_TURTLE.getURI()))
            .map(node -> node.getProperty(accessURL).getResource().getURI())
            .collect(Collectors.toList());
    }

}
