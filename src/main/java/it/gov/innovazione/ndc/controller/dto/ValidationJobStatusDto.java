package it.gov.innovazione.ndc.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.innovazione.ndc.harvester.model.validation.ValidationReport;
import it.gov.innovazione.ndc.service.validation.ValidationJob;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationJobStatusDto {
    private final String validationId;
    private final ValidationJob.Status status;
    private final String owner;
    private final String repo;
    private final String revision;
    private final String repoUrl;
    private final Instant submittedAt;
    private final Instant completedAt;
    private final ValidationJob.Progress progress;
    private final ValidationReport report;
    private final String errorMessage;

    public static ValidationJobStatusDto from(ValidationJob job) {
        return ValidationJobStatusDto.builder()
                .validationId(job.getId())
                .status(job.getStatus())
                .owner(job.getOwner())
                .repo(job.getRepo())
                .revision(job.getRevision())
                .repoUrl(job.getRepoUrl())
                .submittedAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .progress(job.getProgress())
                .report(job.getReport())
                .errorMessage(job.getErrorMessage())
                .build();
    }
}
