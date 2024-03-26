package it.gov.innovazione.ndc.model.harvester;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.With;

import java.time.Instant;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(exclude = {"id", "createdAt", "updatedAt"})
@ToString(exclude = {"rightsHolders"})
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
    @JsonIgnore
    private Map<String, Map<String, String>> rightsHolders;
}
