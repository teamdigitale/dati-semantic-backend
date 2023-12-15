package it.gov.innovazione.ndc.harvester;

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
