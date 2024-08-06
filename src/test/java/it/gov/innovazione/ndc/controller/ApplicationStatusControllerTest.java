package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.gen.dto.Problem;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

class ApplicationStatusControllerTest {

    @Test
    void shouldReportHealthy() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        ApplicationStatusController applicationStatusController =
            new ApplicationStatusController(healthEndpoint);

        ResponseEntity<Problem> status =
            applicationStatusController.getStatus();

        verify(healthEndpoint).health();
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(status.getHeaders().get(CONTENT_TYPE)).get(0))
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(Objects.requireNonNull(status.getBody()).getStatus()).isEqualTo(200);
        assertThat(Objects.requireNonNull(status.getBody()).getTitle()).isEqualTo(
            "Application is available");
    }

    @Test
    void shouldReportUnhealthyWhenDown() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.down().build());
        ApplicationStatusController applicationStatusController =
            new ApplicationStatusController(healthEndpoint);

        ResponseEntity<Problem> status =
            applicationStatusController.getStatus();

        verify(healthEndpoint).health();
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Objects.requireNonNull(status.getHeaders().get(CONTENT_TYPE)).get(0))
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(Objects.requireNonNull(status.getBody()).getStatus()).isEqualTo(500);
        assertThat(Objects.requireNonNull(status.getBody()).getTitle()).isEqualTo(
            "Application is not available");
    }

    @Test
    void shouldReportUnhealthyWhenOutOfService() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.outOfService().build());
        ApplicationStatusController applicationStatusController =
            new ApplicationStatusController(healthEndpoint);

        ResponseEntity<Problem> status =
            applicationStatusController.getStatus();

        verify(healthEndpoint).health();
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Objects.requireNonNull(status.getHeaders().get(CONTENT_TYPE)).get(0))
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(Objects.requireNonNull(status.getBody()).getStatus()).isEqualTo(500);
        assertThat(Objects.requireNonNull(status.getBody()).getTitle()).isEqualTo(
            "Application is not available");
    }

    @Test
    void shouldReportUnhealthyWhenUnknown() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.unknown().build());
        ApplicationStatusController applicationStatusController =
            new ApplicationStatusController(healthEndpoint);

        ResponseEntity<Problem> status =
            applicationStatusController.getStatus();

        verify(healthEndpoint).health();
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Objects.requireNonNull(status.getHeaders().get(CONTENT_TYPE)).get(0))
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(Objects.requireNonNull(status.getBody()).getStatus()).isEqualTo(500);
        assertThat(Objects.requireNonNull(status.getBody()).getTitle()).isEqualTo(
            "Application is not available");
    }
}