package it.teamdigitale.ndc.harvester.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public interface SemanticAssetModel {
    Resource getMainResource();

    Model getRdfModel();
}
