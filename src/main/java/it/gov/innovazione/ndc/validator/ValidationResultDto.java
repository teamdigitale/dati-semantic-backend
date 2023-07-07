package it.gov.innovazione.ndc.validator;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.validator.model.ValidationOutcome;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ValidationResultDto {

    private List<ValidationOutcomeDto> errors;
    private List<ValidationOutcomeDto> warnings;

    public ValidationResultDto(SemanticAssetModelValidationContext context) {
        this.errors = getNormalized(context.getErrors()).stream()
                .map(ValidationOutcomeDto::fromValidationOutcome)
                .distinct()
                .collect(Collectors.toList());
        this.warnings = getNormalized(context.getWarnings()).stream()
                .map(ValidationOutcomeDto::fromValidationOutcome)
                .collect(Collectors.toList());
    }

    private List<ValidationOutcome> getNormalized(List<ValidationOutcome> validationOutcomes) {
        Map<String, List<ValidationOutcome>> byMessage = validationOutcomes.stream()
                .collect(Collectors.groupingBy(ValidationOutcome::getMessage));

        return byMessage.keySet().stream()
                .map(key -> byMessage.get(key).get(0))
                .collect(Collectors.toList());
    }

    @Data
    static class ValidationOutcomeDto {
        private final String fieldName;
        private final String message;

        public static ValidationOutcomeDto fromValidationOutcome(ValidationOutcome validationOutcome) {
            return new ValidationOutcomeDto(
                    validationOutcome.getFieldName(),
                    validationOutcome.getMessage());
        }
    }
}
