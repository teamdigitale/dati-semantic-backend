package it.teamdigitale.ndc.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {AppProblemGeneratingException.class})
    public ResponseEntity<Object> handleExceptionWithAppProblem(AppProblemGeneratingException exception) {
        HttpStatus status = exception.getStatus();
        ApplicationProblem report = exception.buildReport();
        return buildResponseEntityForAppProblem(status, report);
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<Object> handleValidationFailures(ConstraintViolationException ex) {
        String errorMessage = "Validation for parameter failed " + ex.getMessage();
        ApplicationProblem report = ApplicationProblem.builder()
                .type(ApplicationProblem.getErrorUri(ConstraintViolationException.class.getSimpleName()))
                .status(HttpStatus.BAD_REQUEST.value())
                .title(errorMessage)
                .build();
        return buildResponseEntityForAppProblem(HttpStatus.BAD_REQUEST, report);
    }

    private ResponseEntity<Object> buildResponseEntityForAppProblem(HttpStatus status, ApplicationProblem report) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(report);
    }
}
