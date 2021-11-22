package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

@Slf4j
public class ControlledVocabularyModel extends SemanticAssetModel {
    public static final String DATASET_IRI = "http://dati.gov.it/onto/dcatapit#Dataset";
    public static final String KEY_CONCEPT_IRI = "https://w3id.org/italia/onto/ndc-profile/keyConcept";

    public ControlledVocabularyModel(Model coreModel, String source) {
        super(coreModel, source);
    }

    public String getKeyConcept() {
        Property keyConceptProperty = createProperty(KEY_CONCEPT_IRI);
        Resource mainResource = getMainResource();
        StmtIterator stmtIterator = mainResource.listProperties(keyConceptProperty);
        try {
            if (!stmtIterator.hasNext()) {
                log.warn("No key concept ({}) statement for controlled vocabulary '{}'", KEY_CONCEPT_IRI, mainResource);
                throw new InvalidAssetException("No key concept property for controlled vocabulary " + mainResource);
            }

            Statement statement = stmtIterator.nextStatement();
            if (stmtIterator.hasNext()) {
                log.warn("Multiple key concept ({}) statements for controlled vocabulary '{}'", KEY_CONCEPT_IRI, mainResource);
                throw new InvalidAssetException("Multiple key concept properties for controlled vocabulary " + mainResource);
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

    @Override
    protected String getMainResourceIri() {
        return DATASET_IRI;
    }

}
