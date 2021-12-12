package it.teamdigitale.ndc.controller;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationStatusController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping("/status")
    public ResponseEntity<Health> getStatus() {
        Status status = healthEndpoint.health().getStatus();
        if (status != Status.UP) {
            Health response = Health.builder()
                .status(INTERNAL_SERVER_ERROR.value())
                .title("Application is not available")
                .build();
            return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
        }
        return ResponseEntity.status(OK.value())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(Health.builder()
                .status(OK.value())
                .title("Application is available").build());
    }

    @Value
    @Builder
    static class Health {
        int status;
        String title;
    }
}
