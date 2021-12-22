package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public interface SemanticAssetModel {
    Resource getMainResource();

    Model getRdfModel();

    SemanticAssetMetadata extractMetadata();
}
