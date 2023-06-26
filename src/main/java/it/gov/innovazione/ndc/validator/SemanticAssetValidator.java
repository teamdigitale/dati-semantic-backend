package it.gov.innovazione.ndc.validator;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;

public interface SemanticAssetValidator {

    SemanticAssetType getType();

    ValidationResultDto validate(Model resource);
}
