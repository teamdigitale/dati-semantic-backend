package it.teamdigitale.ndc.controller.exception;

import it.teamdigitale.ndc.gen.dto.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    @ParameterizedTest
    @MethodSource("exceptionsToBeTested")
    void shouldExceptionWithAppProblem(ProblemBuildingException exception) {
        RestExceptionHandler handler = new RestExceptionHandler();

        ResponseEntity<Object> responseEntity =
                handler.handleExceptionWithAppProblem(exception);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Object body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isInstanceOf(Problem.class);
        Problem applicationProblem = (Problem) body;
        assertThat(applicationProblem.getTitle()).isEqualTo(exception.getMessage());
    }

    public static Stream<Arguments> exceptionsToBeTested() {
        return Stream.of(
                Arguments.of(new VocabularyDataNotFoundException("testIndex")),
                Arguments.of(new SemanticAssetNotFoundException("http://some.iri")),
                Arguments.of(new VocabularyItemNotFoundException("test.index", "123"))
        );
    }

    @Test
    void shouldHandleValidationException() {
        RestExceptionHandler handler = new RestExceptionHandler();

        ResponseEntity<Object> responseEntity =
                handler.handleValidationFailures(new ConstraintViolationException("1234", Set.of()));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isInstanceOf(Problem.class);
        Problem problem = (Problem) body;
        assertThat(problem.getTitle()).isEqualTo("Validation for parameter failed 1234");
    }
}