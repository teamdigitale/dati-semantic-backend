package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.controller.dto.ValidationJobStatusDto;
import it.gov.innovazione.ndc.controller.dto.ValidationJobSubmittedDto;
import it.gov.innovazione.ndc.service.validation.RepoValidationService;
import it.gov.innovazione.ndc.service.validation.ValidationJob;
import it.gov.innovazione.ndc.service.validation.ValidationJobStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepoValidationControllerTest {

    @Mock
    private RepoValidationService repoValidationService;
    @Mock
    private ValidationJobStore validationJobStore;

    @InjectMocks
    private RepoValidationController controller;

    @Test
    void shouldReturn202WhenValidationSubmitted() {
        when(repoValidationService.tryAcquire()).thenReturn(true);

        ResponseEntity<ValidationJobSubmittedDto> response =
                controller.submitValidation("istat", "ts-ontologie", "main");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRepoUrl())
                .isEqualTo("https://github.com/istat/ts-ontologie");
        assertThat(response.getBody().getStatus()).isEqualTo(ValidationJob.Status.PENDING);
        assertThat(response.getBody().getRevision()).isEqualTo("main");
        verify(validationJobStore).put(any(ValidationJob.class));
        verify(repoValidationService).executeValidation(any(ValidationJob.class));
    }

    @Test
    void shouldReturn429WhenTooManyRequests() {
        when(repoValidationService.tryAcquire()).thenReturn(false);

        ResponseEntity<ValidationJobSubmittedDto> response =
                controller.submitValidation("owner", "repo", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldReturnJobStatus() {
        ValidationJob job = ValidationJob.builder()
                .id("test-id")
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.VALIDATING)
                .build();
        job.setTotalAssets(10);
        job.getProcessedAssets().set(5);

        when(validationJobStore.find("test-id")).thenReturn(Optional.of(job));

        ResponseEntity<ValidationJobStatusDto> response =
                controller.getValidationStatus("test-id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ValidationJob.Status.VALIDATING);
        assertThat(response.getBody().getProgress()).isNotNull();
        assertThat(response.getBody().getProgress().percentage()).isEqualTo(50);
    }

    @Test
    void shouldReturn404ForUnknownValidationId() {
        when(validationJobStore.find("unknown")).thenReturn(Optional.empty());

        ResponseEntity<ValidationJobStatusDto> response =
                controller.getValidationStatus("unknown");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDelegateToOverloadedMethodWithoutRevision() {
        when(repoValidationService.tryAcquire()).thenReturn(true);

        ResponseEntity<ValidationJobSubmittedDto> response =
                controller.submitValidation("owner", "repo", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRevision()).isNull();
    }

    @Test
    void shouldAcceptRevisionFromPathVariant() {
        when(repoValidationService.tryAcquire()).thenReturn(true);

        ResponseEntity<ValidationJobSubmittedDto> response =
                controller.submitValidationWithRevisionPath("owner", "repo", "feature/test");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRevision()).isEqualTo("feature/test");
    }
}
