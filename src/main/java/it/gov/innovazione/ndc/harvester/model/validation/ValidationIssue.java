package it.gov.innovazione.ndc.harvester.model.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationIssue {
    private final String code;
    private final ValidationIssueSeverity severity;
    private final String message;
    private final String name;
    private final String category;
    private final String result;
    private final String details;
    private final String field;
    private final Long line;
    private final Long col;
}
