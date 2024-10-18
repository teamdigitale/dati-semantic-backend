package it.gov.innovazione.ndc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.innovazione.ndc.gen.dto.Problem;
import it.gov.innovazione.ndc.model.Builders;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@Tag(name = "status", description = "Entry point for application status")
public class ApplicationStatusController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping("/status")
    @Operation(
            operationId = "checkStatus",
            description = "Check if the application is available and healthy",
            tags = {"status"},
            summary = "Check the application status")
    public ResponseEntity<Problem> getStatus() {
        Status status = healthEndpoint.health().getStatus();
        if (status != Status.UP) {
            Problem response = Builders.problem()
                    .status(INTERNAL_SERVER_ERROR)
                    .title("Application is not available")
                    .build();
            return ResponseEntity
                    .status(INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(response);
        }
        return ResponseEntity.status(OK.value())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Builders.problem()
                        .status(OK)
                        .title("Application is available").build());
    }
}
