package it.teamdigitale.ndc.controller.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(responseMap.get("message"))
            .isEqualTo("Unable to find vocabulary data for : testIndex");
    }
}