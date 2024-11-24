package it.gov.innovazione.ndc.eventhandler.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
public class HarvesterUpdateCommitDateEvent {
    private final String runId;
    private final Instant commitDate;
}
