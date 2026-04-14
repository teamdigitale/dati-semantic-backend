package it.gov.innovazione.ndc.service.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.innovazione.ndc.harvester.model.validation.ValidationReport;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationJob {
    private final String id;
    private final String owner;
    private final String repo;
    private final String revision;
    private final Instant createdAt;
    private volatile Instant completedAt;
    private volatile Status status;
    private volatile ValidationReport report;
    private volatile String errorMessage;
    @JsonIgnore
    @Builder.Default
    private final AtomicInteger processedAssets = new AtomicInteger(0);
    @Builder.Default
    private volatile int totalAssets = -1;
    @JsonIgnore
    private volatile Instant lastAccessedAt;

    public Progress getProgress() {
        if (totalAssets < 0) {
            return null;
        }
        int processed = processedAssets.get();
        int percentage = totalAssets > 0 ? (processed * 100) / totalAssets : 0;
        return new Progress(processed, totalAssets, percentage);
    }

    public String getRepoUrl() {
        return "https://github.com/" + owner + "/" + repo;
    }

    public enum Status {
        PENDING, CLONING, DISCOVERING, VALIDATING, COMPLETED, FAILED
    }

    public record Progress(int processedAssets, int totalAssets, int percentage) {
    }
}
