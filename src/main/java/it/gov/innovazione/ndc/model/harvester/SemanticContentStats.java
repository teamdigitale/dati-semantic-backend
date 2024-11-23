package it.gov.innovazione.ndc.model.harvester;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
public class SemanticContentStats {
    private final String id;
    private final String harvesterRunId;
    private final String resourceUri;
    private final SemanticAssetType resourceType;
    private final String rightHolder;
    private final LocalDate issuedOn;
    private final LocalDate modifiedOn;
    private final boolean hasErrors;
    private final boolean hasWarnings;
    private final List<String> status;
}
