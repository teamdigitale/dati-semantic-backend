package it.teamdigitale.ndc.harvester.model.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class EuropaVocabulary {

    public static final Property RDF_TURTLE =
        ResourceFactory.createProperty(
            "http://publications.europa.eu/resource/authority/file-type/RDF_TURTLE");

    public static final Property JSON =
        ResourceFactory.createProperty(
            "http://publications.europa.eu/resource/authority/file-type/JSON");
}
