package it.gov.innovazione.ndc.model.harvester;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Data
@Builder
@RequiredArgsConstructor
public class HarvesterRun {
    private final String id;
    private final String correlationId;
    private final String repositoryId;
    private final String repositoryUrl;
    private final Instant startedAt;
    private final String startedBy;
    private final Instant endedAt;
    private final String revision;
    private final Status status;
    private final String reason;

    public enum Status {
        SUCCESS,
        UNCHANGED,
        RUNNING, FAILED
    }

}
