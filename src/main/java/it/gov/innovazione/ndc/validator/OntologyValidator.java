package it.gov.innovazione.ndc.validator;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.OntologyModel;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

@Component
public class OntologyValidator extends BaseSemanticAssetValidator<OntologyModel> {

    public OntologyValidator() {
        super(SemanticAssetType.ONTOLOGY);
    }

    @Override
    protected OntologyModel getValidatorModel(Model rdfModel) {
        return OntologyModel.forValidation(rdfModel, null, null);
    }
}
