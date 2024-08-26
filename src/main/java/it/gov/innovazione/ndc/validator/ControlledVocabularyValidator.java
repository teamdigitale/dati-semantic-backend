package it.gov.innovazione.ndc.validator;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.ControlledVocabularyModel;
import it.gov.innovazione.ndc.harvester.model.Instance;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

@Component
public class ControlledVocabularyValidator extends BaseSemanticAssetValidator<ControlledVocabularyModel> {

    public ControlledVocabularyValidator() {
        super(SemanticAssetType.CONTROLLED_VOCABULARY);
    }

    @Override
    protected ControlledVocabularyModel getValidatorModel(Model rdfModel) {
        return ControlledVocabularyModel.forValidation(rdfModel, null, null, Instance.PRIMARY);
    }
}
