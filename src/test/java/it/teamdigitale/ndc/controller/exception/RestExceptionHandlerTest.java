package it.teamdigitale.ndc.controller.exception;

import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolationException;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RestExceptionHandlerTest {

    @Test
    void shouldHandleVocabularyNotFound() {
        RestExceptionHandler handler = new RestExceptionHandler();
        ResponseEntity<Object> responseEntity =
                handler.handleNotFound(new VocabularyDataNotFoundException("testIndex"));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Object body = responseEntity.getBody();
        assertThat(body).isInstanceOf(Map.class);
        assertThat(body).isNotNull();
        Map<String, String> responseMap = (Map<String, String>) body;
        assertThat(responseMap.get("message"))
                .isEqualTo("Unable to find vocabulary data for : testIndex");
    }

    @Test
    void shouldHandleValidationException() {
        RestExceptionHandler handler = new RestExceptionHandler();
        ResponseEntity<Object> responseEntity =
                handler.handleValidationFailures(new ConstraintViolationException("1234", Set.of()));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object body = responseEntity.getBody();
        assertThat(body).isInstanceOf(Map.class);
        assertThat(body).isNotNull();
        Map<String, String> responseMap = (Map<String, String>) body;
        assertThat(responseMap.get("message")).isEqualTo("Validation for parameter failed 1234");
    }
}