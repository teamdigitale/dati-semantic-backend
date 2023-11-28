package it.gov.innovazione.ndc.eventhandler;

import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class HarvesterFinishedEvent implements NdcEventWrapper.NdcEvent {
    private final String runId;
    private final Repository repository;
    private final String revision;
    private final Status status;
    private final Exception exception;

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
