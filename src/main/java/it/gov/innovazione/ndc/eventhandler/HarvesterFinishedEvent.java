package it.gov.innovazione.ndc.eventhandler;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class HarvesterFinishedEvent {
    private final String runId;
    private final Repository repository;
    private final String revision;
    private final HarvesterRun.Status status;
    private final Exception exception;
}
