package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import it.teamdigitale.ndc.harvester.model.exception.InvalidModelException;
import static it.teamdigitale.ndc.harvester.model.extractors.NodeExtractor.extractNodes;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import java.util.List;
import static java.util.Objects.isNull;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import static org.apache.jena.vocabulary.DCAT.distribution;
import org.apache.jena.vocabulary.DCTerms;

@Slf4j
public class ControlledVocabularyModel extends BaseSemanticAssetModel {
    public static final String NDC_PREFIX = "https://w3id.org/italia/onto/ndc-profile/";
    public static final String KEY_CONCEPT_IRI = NDC_PREFIX + "keyConcept";
    public static final String REST_ENDPOINT_IRI = NDC_PREFIX + "endpointUrl";
    public static final String NDC_ENDPOINT_URL = "%s/vocabularies/%s/%s";

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

    public String getRightsHolderId() {
        return getMainResource()
            .getRequiredProperty(DCTerms.rightsHolder)
            .getProperty(DCTerms.identifier)
            .getString();
    }

    public void addNdcUrlProperty(String baseUrl) {
        Property endpointUrlProperty = createProperty(REST_ENDPOINT_IRI);
        String ndcUrl = String.format(NDC_ENDPOINT_URL, baseUrl, getRightsHolderId(), getKeyConcept());

        this.getMainResource().addProperty(endpointUrlProperty, ndcUrl);
    }

    @Override
    protected String getMainResourceTypeIri() {
        return CONTROLLED_VOCABULARY.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder()
                .distributionUrls(getDistributionUrls())
                .keyConcept(getKeyConcept())
                .endpointUrl(getNdcEndpointUrl()) // assumption is that ndc endpoint url will be added to model before indexing
                .build();
    }

    private List<String> getDistributionUrls() {
        return extractNodes(getMainResource(), distribution).stream().map(Resource::getURI).collect(Collectors.toList());
    }

    private String getNdcEndpointUrl() {
        Property endpointUrlProperty = createProperty(REST_ENDPOINT_IRI);
        Statement endpointUrl = getMainResource().getProperty(endpointUrlProperty);
        return isNull(endpointUrl) ? "" : endpointUrl.getString();
    }
}
