package it.teamdigitale.ndc.harvester.model;

import static it.teamdigitale.ndc.harvester.SemanticAssetType.ONTOLOGY;

import org.apache.jena.rdf.model.Model;

public class OntologyModel extends BaseSemanticAssetModel {
    public OntologyModel(Model coreModel, String source, String repoUrl) {
        super(coreModel, source, repoUrl);
    }

    @Override
    protected String getMainResourceTypeIri() {
        return ONTOLOGY.getTypeIri();
    }
}
