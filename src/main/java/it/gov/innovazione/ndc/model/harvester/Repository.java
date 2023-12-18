package it.gov.innovazione.ndc.model.harvester;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(exclude = {"id", "createdAt", "updatedAt"})
public class Repository {
    private String id;
    private String url;
    private String name;
    private String description;
    private String owner;
    private Boolean active;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long maxSizeBytes;
}
