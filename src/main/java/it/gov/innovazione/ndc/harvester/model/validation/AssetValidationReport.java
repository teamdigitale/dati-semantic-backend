package it.gov.innovazione.ndc.harvester.model.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetValidationReport {
    private final String assetPath;
    private final String assetType;
    @Singular
    private final List<ValidationIssue> issues;
    private final boolean skipped;
}
