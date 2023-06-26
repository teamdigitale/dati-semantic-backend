package it.gov.innovazione.ndc.validator;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.BaseSemanticAssetModel;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;

@RequiredArgsConstructor
public abstract class BaseSemanticAssetValidator<M extends BaseSemanticAssetModel> implements SemanticAssetValidator {

    private final SemanticAssetType type;

    @Override
    public SemanticAssetType getType() {
        return type;
    }

    @Override
    public ValidationResultDto validate(Model resource) {
        return new ValidationResultDto(
                getValidatorModel(resource)
                        .validateMetadata());
    }

    protected abstract M getValidatorModel(Model rdfModel);

}
