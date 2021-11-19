package it.teamdigitale.ndc.harvester;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.util.List;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Component;

@Component
public class SemanticAssetsParser {

    public Resource getControlledVocabulary(String ttlFile) {
        List<Resource> resources = RDFDataMgr.loadModel(ttlFile, Lang.TURTLE)
            .listResourcesWithProperty(RDF.type,
                createResource("http://dati.gov.it/onto/dcatapit#Dataset"))
            .toList();

        assert resources.size() == 1;
        return resources.get(0);
    }

    public String getKeyConcept(Resource controlledVocabulary) {
        return controlledVocabulary.getRequiredProperty(
                createProperty("https://w3id.org/italia/onto/NDC/keyConcept"))
            .getString();
    }

    public String getRightsHolderId(Resource controlledVocabulary) {
        return controlledVocabulary.getRequiredProperty(DCTerms.rightsHolder)
            .getProperty(DCTerms.identifier)
            .getString();
    }
}
