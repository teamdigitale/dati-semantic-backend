package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

@Component
@Slf4j
public class SemanticAssetsParser {

    public static final String DATASET_IRI = "http://dati.gov.it/onto/dcatapit#Dataset";
    public static final String ONTOLOGY_IRI = "http://www.w3.org/2002/07/owl#Ontology";

    public Resource getOntology(String ttlFile) {
        return getUniqueResourceByType(ttlFile, ONTOLOGY_IRI);
    }

    public String getRightsHolderId(Resource controlledVocabulary) {
        return controlledVocabulary
                .getRequiredProperty(DCTerms.rightsHolder)
                .getProperty(DCTerms.identifier)
                .getString();
    }

    private Resource getUniqueResourceByType(String ttlFile, String resourceTypeIri) {
        List<Resource> resources =
                RDFDataMgr.loadModel(ttlFile, Lang.TURTLE)
                        .listResourcesWithProperty(RDF.type, createResource(resourceTypeIri))
                        .toList();

        checkFileDeclaresSingleResource(resources, ttlFile, resourceTypeIri);
        return resources.get(0);
    }

    private void checkFileDeclaresSingleResource(List<Resource> resources, String ttlFile, String typeIri) {
        if (resources.size() == 1) {
            return;
        }
    }
}
