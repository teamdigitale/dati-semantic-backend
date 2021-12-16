package it.teamdigitale.ndc.controller.exception;

import it.teamdigitale.ndc.gen.dto.Problem;
import it.teamdigitale.ndc.model.Builders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {ProblemBuildingException.class})
    public ResponseEntity<Object> handleExceptionWithAppProblem(ProblemBuildingException exception) {
        HttpStatus status = exception.getStatus();
        Problem report = exception.buildReport();
        return buildResponseEntityForAppProblem(status, report);
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<Object> handleValidationFailures(ConstraintViolationException ex) {
        String errorMessage = "Validation for parameter failed " + ex.getMessage();
        Problem report = Builders.problem()
                .errorClass(ConstraintViolationException.class.getSimpleName())
                .status(HttpStatus.BAD_REQUEST)
                .title(errorMessage)
                .build();
        return buildResponseEntityForAppProblem(HttpStatus.BAD_REQUEST, report);
    }

    private ResponseEntity<Object> buildResponseEntityForAppProblem(HttpStatus status, Problem report) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(report);
    }
}
