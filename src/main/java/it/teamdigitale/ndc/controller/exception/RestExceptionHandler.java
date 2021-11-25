package it.teamdigitale.ndc.controller.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {VocabularyDataNotFoundException.class,
        SemanticAssetNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", exception.getMessage());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    protected ResponseEntity<Object> handleValidationFailures(ConstraintViolationException ex) {
        String errorMessage = "Validation for parameter failed " + ex.getMessage();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", errorMessage);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
