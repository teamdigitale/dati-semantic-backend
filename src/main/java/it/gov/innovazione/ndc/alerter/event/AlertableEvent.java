package it.gov.innovazione.ndc.alerter.event;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;

import java.time.Instant;
import java.util.Map;

public interface AlertableEvent {
    String getName();

    String getDescription();

    default Instant getOccurredAt() {
        return Instant.now();
    }

    EventCategory getCategory();

    Severity getSeverity();

    Map<String, Object> getContext();
}
