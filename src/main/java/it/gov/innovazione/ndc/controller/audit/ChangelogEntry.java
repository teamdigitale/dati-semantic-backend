package it.gov.innovazione.ndc.controller.audit;

import com.fasterxml.jackson.databind.JsonNode;
import it.gov.innovazione.ndc.model.audit.ChangeKind;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ChangelogEntry {
    private final String runId;
    private final String repositoryId;
    private final String revision;
    private final Instant revisionCommittedAt;
    private final Instant createdAt;
    private final ChangeKind changeKind;
    private final JsonNode summary;
}
