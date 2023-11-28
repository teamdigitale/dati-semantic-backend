package it.gov.innovazione.ndc.model.harvester;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"id", "createdAt", "updatedAt"})
public class Repository {
    private final String id;
    private final String url;
    private final String name;
    private final String description;
    private final String owner;
    private final Boolean active;
    private final Instant createdAt;
    private final String createdBy;
    private final Instant updatedAt;
    private final String updatedBy;
    private final Source source;

    public enum Source {
        CONFIG,
        DATABASE
    }
}
