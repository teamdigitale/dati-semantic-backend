package it.gov.innovazione.ndc.controller.exception;

import it.gov.innovazione.ndc.gen.dto.Problem;
import it.gov.innovazione.ndc.model.Builders;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {ProblemBuildingException.class})
    public ResponseEntity<Object> handleExceptionWithAppProblem(ProblemBuildingException exception) {
        log.error("ProblemBuildingException: ", exception);
        HttpStatus status = exception.getStatus();
        Problem report = exception.buildReport();
        return buildResponseEntityForAppProblem(status, report);
    }

    @ExceptionHandler(value = {ConstraintViolationException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Object> handleValidationFailures(RuntimeException ex) {
        log.error("ValidationFailure: ", ex);
        String errorMessage = "Validation for parameter failed";
        Problem report = Builders.problem()
                .errorClass(ex.getClass().getSimpleName())
                .status(HttpStatus.BAD_REQUEST)
                .title(errorMessage)
                .build();
        return buildResponseEntityForAppProblem(HttpStatus.BAD_REQUEST, report);
    }

    private ResponseEntity<Object> buildResponseEntityForAppProblem(HttpStatus status, Problem report) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(report);
    }
}
