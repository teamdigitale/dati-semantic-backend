package it.gov.innovazione.ndc.eventhandler.event;

import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class HarvesterStartedEvent {
    private final String runId;
    private final Repository repository;
    private final String revision;
}
