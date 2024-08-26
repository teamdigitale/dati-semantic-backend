package it.gov.innovazione.ndc.eventhandler.event;

import it.gov.innovazione.ndc.harvester.model.Instance;
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
    private final Instance instance;
    private final String revision;

    @Override
    public String getName() {
        return "Run " + runId + " started on instance " + instance;
    }

    @Override
    public String getDescription() {
        return "Run " + runId + " started on instance " + instance + " for repository " + repository.getUrl() + " with revision " + revision;
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
