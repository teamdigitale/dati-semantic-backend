package it.gov.innovazione.ndc.alerter.dto;

import it.gov.innovazione.ndc.alerter.entities.Nameable;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

@Data
public class ProfileDto implements Nameable {
    private String id;
    @NotBlank(message = "Name is mandatory")
    private String name;
    @NotEmpty(message = "Event categories are mandatory")
    private List<String> eventCategories;
    private Severity minSeverity = Severity.INFO;
    private Integer aggregationTime = 60;
    private Instant lastAlertedAt;
}
