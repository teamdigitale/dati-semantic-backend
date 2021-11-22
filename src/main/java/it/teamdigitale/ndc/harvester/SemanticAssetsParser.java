package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

@Component
@Slf4j
public class SemanticAssetsParser {

    public static final String DATASET_IRI = "http://dati.gov.it/onto/dcatapit#Dataset";
    public static final String KEYCONCEPT_IRI = "https://w3id.org/italia/onto/ndc-profile/keyConcept";

    public Resource getControlledVocabulary(String ttlFile) {
        List<Resource> resources =
                RDFDataMgr.loadModel(ttlFile, Lang.TURTLE)
                        .listResourcesWithProperty(RDF.type, createResource(DATASET_IRI))
                        .toList();

        checkFileDeclaresSingleControlledVocabulary(ttlFile, resources);
        return resources.get(0);
    }

    private void checkFileDeclaresSingleControlledVocabulary(String ttlFile, List<Resource> resources) {
        if (resources.size() == 1) {
            return;
        }

        if (resources.isEmpty()) {
            throw new InvalidAssetException(
                    format("No statement for a node whose type is '%s' in '%s'", DATASET_IRI, ttlFile));
        }
        throw new InvalidAssetException(
                format(
                        "Found %d statements for nodes whose type is '%s' in '%s', expecting only 1",
                        resources.size(), DATASET_IRI, ttlFile));
    }

    public String getKeyConcept(Resource controlledVocabulary) {
        Property keyConceptProperty = createProperty(KEYCONCEPT_IRI);
        StmtIterator stmtIterator = controlledVocabulary.listProperties(keyConceptProperty);
        try {
            if (!stmtIterator.hasNext()) {
                log.warn("No key concept ({}) statement for controlled vocabulary '{}'", KEYCONCEPT_IRI, controlledVocabulary);
                throw new InvalidAssetException("No key concept property for controlled vocabulary " + controlledVocabulary);
            }

            Statement statement = stmtIterator.nextStatement();
            if (stmtIterator.hasNext()) {
                log.warn("Multiple key concept ({}) statements for controlled vocabulary '{}'", KEYCONCEPT_IRI, controlledVocabulary);
                throw new InvalidAssetException("Multiple key concept properties for controlled vocabulary " + controlledVocabulary);
            }

            return statement.getObject().toString();
        } finally {
            stmtIterator.close();
        }
    }

    public String getRightsHolderId(Resource controlledVocabulary) {
        return controlledVocabulary
                .getRequiredProperty(DCTerms.rightsHolder)
                .getProperty(DCTerms.identifier)
                .getString();
    }
}
