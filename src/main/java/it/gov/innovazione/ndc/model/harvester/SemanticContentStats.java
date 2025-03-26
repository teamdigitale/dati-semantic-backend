package it.gov.innovazione.ndc.model.harvester;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @With
    @JsonIgnore
    private final HarvesterRun harvesterRun;

    public String getStatusType() {
        Set<String> lowerCaseStatus = status.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (lowerCaseStatus.contains("archived")) {
            return "Archiviato";
        } else if (lowerCaseStatus.contains("catalogued") && lowerCaseStatus.contains("published")) {
            return "Stabile";
        } else if (lowerCaseStatus.contains("closed access")) {
            return "Accesso Ristretto";
        } else if (lowerCaseStatus.contains("initial draft")
                || lowerCaseStatus.contains("draft")
                || lowerCaseStatus.contains("final draft")
                || lowerCaseStatus.contains("intermediate draft")
                || lowerCaseStatus.contains("submitted")) {
            return "Bozza";
        }
        return "unknown";
    }

}
