package it.gov.innovazione.ndc.validator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationOutcome {

    private String fieldName;

    private String message;

    private RuntimeException exception;

}
