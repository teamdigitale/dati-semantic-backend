package it.gov.innovazione.ndc.validator;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.SchemaModel;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

@Component
public class SchemaValidator extends BaseSemanticAssetValidator<SchemaModel> {

    public SchemaValidator() {
        super(SemanticAssetType.SCHEMA);
    }

    @Override
    protected SchemaModel getValidatorModel(Model rdfModel) {
        return SchemaModel.forValidation(rdfModel, null, null);
    }
}
