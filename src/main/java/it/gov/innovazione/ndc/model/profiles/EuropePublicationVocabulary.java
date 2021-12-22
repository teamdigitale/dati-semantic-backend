package it.gov.innovazione.ndc.model.profiles;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class EuropePublicationVocabulary {

    public static final Property FILE_TYPE_RDF_TURTLE =
        ResourceFactory.createProperty(
            "http://publications.europa.eu/resource/authority/file-type/RDF_TURTLE");

    public static final Property FILE_TYPE_JSON =
        ResourceFactory.createProperty(
            "http://publications.europa.eu/resource/authority/file-type/JSON");
}
