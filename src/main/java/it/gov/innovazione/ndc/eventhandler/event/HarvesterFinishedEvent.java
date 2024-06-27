package it.gov.innovazione.ndc.eventhandler.event;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class HarvesterFinishedEvent implements AlertableEvent {
    private final String runId;
    private final Repository repository;
    private final String revision;
    private final HarvesterRun.Status status;
    private final Exception exception;

    @Override
    public String getName() {
        return "Run " + runId + " finished";
    }

    @Override
    public String getDescription() {
        return "Harvester run " + runId + " finished with status " + status;
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.APPLICATION;
    }

    @Override
    public Severity getSeverity() {
        if (status == HarvesterRun.Status.ALREADY_RUNNING) {
            return Severity.WARNING;
        } else if (status == HarvesterRun.Status.SUCCESS || status == HarvesterRun.Status.UNCHANGED) {
            return Severity.INFO;
        } else if (status == HarvesterRun.Status.FAILURE) {
            return Severity.ERROR;
        }
        return Severity.INFO;
    }

    @Override
    public Map<String, Object> getContext() {
        return Map.of(
                "runId", runId,
                "repository", repository,
                "revision", revision,
                "status", status,
                "exception", exception
        );
    }
}
