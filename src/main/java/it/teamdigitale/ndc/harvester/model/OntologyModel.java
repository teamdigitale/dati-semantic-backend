package it.teamdigitale.ndc.harvester.model;

import org.apache.jena.rdf.model.Model;

public class OntologyModel extends SemanticAssetModel {
    public static final String ONTOLOGY_IRI = "http://www.w3.org/2002/07/owl#Ontology";

    public OntologyModel(Model coreModel, String source) {
        super(coreModel, source);
    }

    @Override
    protected String getMainResourceIri() {
        return ONTOLOGY_IRI;
    }
}
