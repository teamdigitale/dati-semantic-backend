package it.gov.innovazione.ndc.model.harvester;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.service.ActualConfigService;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(exclude = {"id", "createdAt", "updatedAt"})
public class Repository {
    private String id;
    @With
    private String url;
    private String name;
    private String description;
    private String owner;
    private Boolean active;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long maxFileSizeBytes;
    private Map<ActualConfigService.ConfigKey, ConfigService.ConfigEntry> config;
    @JsonIgnore
    private Map<String, Map<String, String>> rightsHolders;
    private List<Maintainer> maintainers;

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) %s %s", id, name, url, active ? "active" : "inactive",
                Optional.ofNullable(config)
                        .map(Object::toString)
                        .orElse(""));
    }

    @Data
    public static class Maintainer {
        private final String name;
        private final String email;
        private final String git;
    }
}
