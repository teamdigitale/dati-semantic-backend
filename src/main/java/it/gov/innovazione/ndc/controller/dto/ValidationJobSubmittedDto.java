package it.gov.innovazione.ndc.controller.dto;

import it.gov.innovazione.ndc.service.validation.ValidationJob;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ValidationJobSubmittedDto {
    private final String validationId;
    private final ValidationJob.Status status;
    private final String repoUrl;
    private final String revision;
    private final Instant submittedAt;

    public static ValidationJobSubmittedDto from(ValidationJob job) {
        return ValidationJobSubmittedDto.builder()
                .validationId(job.getId())
                .status(job.getStatus())
                .repoUrl(job.getRepoUrl())
                .revision(job.getRevision())
                .submittedAt(job.getCreatedAt())
                .build();
    }
}
