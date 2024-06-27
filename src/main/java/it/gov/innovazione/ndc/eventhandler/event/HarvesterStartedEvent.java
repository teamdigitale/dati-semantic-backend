package it.gov.innovazione.ndc.eventhandler.event;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class HarvesterStartedEvent implements AlertableEvent {
    private final String runId;
    private final Repository repository;
    private final String revision;

    @Override
    public String getName() {
        return "Run " + runId + " started";
    }

    @Override
    public String getDescription() {
        return "Harvester run " + runId + " started";
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.APPLICATION;
    }

    @Override
    public Severity getSeverity() {
        return Severity.INFO;
    }

    @Override
    public Map<String, Object> getContext() {
        return Map.of(
                "repository", repository,
                "revision", revision,
                "runId", runId
        );
    }

}
