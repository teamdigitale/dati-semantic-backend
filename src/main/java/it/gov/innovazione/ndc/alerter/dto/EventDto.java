package it.gov.innovazione.ndc.alerter.dto;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Nameable;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class EventDto implements Nameable {
    private String id;
    @NotBlank(message = "Name is mandatory")
    private String name;
    @NotBlank(message = "Description is mandatory")
    private String description;
    @NotNull
    private EventCategory category;
    @Builder.Default
    private Severity severity = Severity.INFO;
    @Builder.Default
    private Map<String, Object> context = Map.of();
    @Builder.Default
    private Instant occurredAt = Instant.now();
    private Instant createdAt;
    @Builder.Default
    private String createdBy = "system";
}
