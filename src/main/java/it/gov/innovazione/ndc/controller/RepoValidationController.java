package it.gov.innovazione.ndc.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.gov.innovazione.ndc.controller.dto.ValidationJobStatusDto;
import it.gov.innovazione.ndc.controller.dto.ValidationJobSubmittedDto;
import it.gov.innovazione.ndc.service.validation.RepoValidationService;
import it.gov.innovazione.ndc.service.validation.ValidationJob;
import it.gov.innovazione.ndc.service.validation.ValidationJobStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/validate/repo")
@RequiredArgsConstructor
public class RepoValidationController {

    private final RepoValidationService repoValidationService;
    private final ValidationJobStore validationJobStore;

    @PostMapping("/{owner}/{repo}")
    @Operation(
            operationId = "submitRepoValidation",
            description = "Submit a validation job for a GitHub repository",
            summary = "Submit async validation for a GitHub repository")
    public ResponseEntity<ValidationJobSubmittedDto> submitValidation(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false) String revision) {
        return submitValidationInternal(owner, repo, revision);
    }

    @PostMapping("/{owner}/{repo}/{revision:.+}")
    @Operation(
            operationId = "submitRepoValidationWithRevision",
            description = "Submit a validation job for a specific revision of a GitHub repository",
            summary = "Submit async validation for a GitHub repository at a specific revision")
    public ResponseEntity<ValidationJobSubmittedDto> submitValidationWithRevisionPath(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String revision) {
        return submitValidationInternal(owner, repo, revision);
    }

    private ResponseEntity<ValidationJobSubmittedDto> submitValidationInternal(
            String owner,
            String repo,
            String revision) {
        if (!repoValidationService.tryAcquire()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        ValidationJob job = ValidationJob.builder()
                .id(UUID.randomUUID().toString())
                .owner(owner)
                .repo(repo)
                .revision(revision)
                .createdAt(Instant.now())
                .status(ValidationJob.Status.PENDING)
                .build();

        validationJobStore.put(job);
        repoValidationService.executeValidation(job);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ValidationJobSubmittedDto.from(job));
    }

    @GetMapping("/{validationId}")
    @Operation(
            operationId = "getValidationStatus",
            description = "Get the status and result of a validation job",
            summary = "Poll validation job status")
    public ResponseEntity<ValidationJobStatusDto> getValidationStatus(
            @PathVariable String validationId) {
        return validationJobStore.find(validationId)
                .map(job -> ResponseEntity.ok(ValidationJobStatusDto.from(job)))
                .orElse(ResponseEntity.notFound().build());
    }
}
