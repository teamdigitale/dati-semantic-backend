package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.CONTROLLED_VOCABULARY;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

@Slf4j
public class ControlledVocabularyModel extends BaseSemanticAssetModel {

    private static final String NDC_API_URI = "https://w3id.org/italia/controlled-vocabulary-api/";
    private static final String NS_NDC_AP = "https://w3id.org/italia/onto/ndc-profile/";
    private static final String NDC_KEY_CONCEPT = NS_NDC_AP + "keyConcept";
    public static final String KEY_CONCEPT_IRI = NDC_KEY_CONCEPT;
    private static final String NDC_AP_DATASERVICE = NS_NDC_AP + "DataService";
    private static final String NDC_AP_ENDPOINTURL = NS_NDC_AP + "endpointUrl";
    private static final String NDC_AP_SERVESDATASET = NS_NDC_AP + "servesDataset";

    public ControlledVocabularyModel(Model coreModel, String source) {
        super(coreModel, source);
    }

    public String getKeyConcept() {
        Property keyConceptProperty = createProperty(NDC_KEY_CONCEPT);
        Resource mainResource = getMainResource();
        StmtIterator stmtIterator = mainResource.listProperties(keyConceptProperty);
        try {
            if (!stmtIterator.hasNext()) {
                log.warn("No key concept ({}) statement for controlled vocabulary '{}'",
                    NDC_KEY_CONCEPT, mainResource);
                throw new InvalidAssetException(
                    "No key concept property for controlled vocabulary " + mainResource);
            }

            Statement statement = stmtIterator.nextStatement();
            if (stmtIterator.hasNext()) {
                log.warn("Multiple key concept ({}) statements for controlled vocabulary '{}'",
                    NDC_KEY_CONCEPT, mainResource);
                throw new InvalidAssetException(
                    "Multiple key concept properties for controlled vocabulary " + mainResource);
            }

            return statement.getObject().toString();
        } finally {
            stmtIterator.close();
        }
    }

    /**
     * Return the associated dataservice resource according to the NDC AP profile.
     */
    public Model dataServiceResource() {
        String endpointUrl = NDC_API_URI + getRightsHolderId() + "/" +  getKeyConcept();

        // Create an empty model so we don't have side effects on the core model.
        Model model = ModelFactory.createDefaultModel();

        model.createResource(endpointUrl)
            .addProperty(RDF.type, model.createResource(NDC_AP_DATASERVICE))
            .addProperty(
                createProperty(NDC_AP_ENDPOINTURL),
                model.createResource(endpointUrl)
            )
            .addProperty(
                createProperty(NDC_AP_SERVESDATASET),
                getMainResource()
            );

        return model;

    }

    public String getRightsHolderId() {
        return getMainResource()
            .getRequiredProperty(DCTerms.rightsHolder)
            .getProperty(DCTerms.identifier)
            .getString();
    }

    @Override
    protected String getMainResourceTypeIri() {
        return CONTROLLED_VOCABULARY.getTypeIri();
    }

    @Override
    public SemanticAssetMetadata extractMetadata() {
        return super.extractMetadata().toBuilder().keyConcept(getKeyConcept()).build();
    }
}
