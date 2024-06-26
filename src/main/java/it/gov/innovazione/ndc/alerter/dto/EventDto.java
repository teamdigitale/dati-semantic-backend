package it.gov.innovazione.ndc.alerter.dto;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Nameable;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

@Data
public class EventDto implements Nameable {
    private String id;
    @NotBlank(message = "Name is mandatory")
    private String name;
    @NotBlank(message = "Description is mandatory")
    private String description;
    @NotNull
    private EventCategory category;
    private Severity severity = Severity.INFO;
    private Map<String, Object> context = Map.of();
    private Instant occurredAt = Instant.now();
    private Instant createdAt;
    private String createdBy = "system";
}
