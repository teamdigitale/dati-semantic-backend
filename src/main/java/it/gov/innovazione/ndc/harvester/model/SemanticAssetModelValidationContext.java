package it.gov.innovazione.ndc.harvester.model;

import it.gov.innovazione.ndc.validator.model.ValidationOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class SemanticAssetModelValidationContext {

    public static final SemanticAssetModelValidationContext NO_VALIDATION = builder()
            .isValidation(false)
            .build();

    @Builder.Default
    private List<ValidationOutcome> errors = new ArrayList<>();
    @Builder.Default
    private List<ValidationOutcome> warnings = new ArrayList<>();
    @Builder.Default
    private Boolean isValidation = true;
    @Builder.Default
    private String fieldName = "";
    @Builder.Default
    private ValidationContextType validationContextType = ValidationContextType.ERROR;

    public static SemanticAssetModelValidationContext getForValidation() {
        return builder().build();
    }

    public static ValidationContextStats difference(ValidationContextStats contextBefore, ValidationContextStats contextAfter) {
        return ValidationContextStats.builder()
                .errors(contextAfter.getErrors() - contextBefore.getErrors())
                .warnings(contextAfter.getWarnings() - contextBefore.getWarnings())
                .build();
    }

    public SemanticAssetModelValidationContext field(String fieldName) {
        return toBuilder().fieldName(fieldName).build();
    }

    public static SemanticAssetModelValidationContext merge(
            SemanticAssetModelValidationContext a, SemanticAssetModelValidationContext b) {
        return SemanticAssetModelValidationContext.builder()
                .errors(
                        getNormalized(
                                Stream.concat(
                                                a.getErrors().stream(),
                                                b.getErrors().stream())
                                        .collect(Collectors.toList())))
                .warnings(
                        getNormalized(
                                Stream.concat(
                                                a.getWarnings().stream(),
                                                b.getWarnings().stream())
                                        .collect(Collectors.toList())))
                .isValidation(a.isValidation)
                .fieldName(a.fieldName)
                .build();
    }

    private static List<ValidationOutcome> getNormalized(List<ValidationOutcome> validationOutcomes) {
        Map<String, List<ValidationOutcome>> byMessage = validationOutcomes.stream()
                .collect(Collectors.groupingBy(ValidationOutcome::getMessage));

        return byMessage.keySet().stream()
                .map(key -> byMessage.get(key).get(0))
                .collect(Collectors.toList());
    }

    public synchronized void addValidationException(RuntimeException invalidModelException) {
        if (validationContextType == ValidationContextType.ERROR) {
            if (getNormalizedErrors().contains(invalidModelException.getMessage())) {
                return;
            }
            errors.add(new ValidationOutcome(fieldName, invalidModelException.getMessage(), invalidModelException));
        } else {
            if (getNormalizedWarnings().contains(invalidModelException.getMessage())) {
                return;
            }
            warnings.add(new ValidationOutcome(fieldName, invalidModelException.getMessage(), invalidModelException));
        }
    }

    public Set<String> getNormalizedErrors() {
        return errors.stream()
                .map(ValidationOutcome::getException)
                .map(Throwable::getMessage)
                .collect(Collectors.toSet());
    }

    public Set<String> getNormalizedWarnings() {
        return warnings.stream()
                .map(ValidationOutcome::getException)
                .map(Throwable::getMessage)
                .collect(Collectors.toSet());
    }

    public SemanticAssetModelValidationContext warning() {
        setWarningValidationType();
        return this;
    }

    public SemanticAssetModelValidationContext error() {
        setErrorValidationType();
        return this;
    }

    public void setWarningValidationType() {
        setValidationContextType(ValidationContextType.WARNING);
    }

    public void setErrorValidationType() {
        setValidationContextType(ValidationContextType.ERROR);
    }

    private enum ValidationContextType {
        ERROR, WARNING
    }

    @Data
    @Builder
    public static class ValidationContextStats {
        private final int errors;
        private final int warnings;

        public static ValidationContextStats of(SemanticAssetModelValidationContext validationContext) {
            return ValidationContextStats.builder()
                    .errors(validationContext.getErrors().size())
                    .warnings(validationContext.getWarnings().size())
                    .build();
        }

        public static ValidationContextStats empty() {
            return ValidationContextStats.builder()
                    .errors(0)
                    .warnings(0)
                    .build();
        }
    }
}
