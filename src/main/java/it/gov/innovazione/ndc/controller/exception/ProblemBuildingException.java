package it.gov.innovazione.ndc.controller.exception;

import it.gov.innovazione.ndc.gen.dto.Problem;
import it.gov.innovazione.ndc.model.Builders;
import org.springframework.http.HttpStatus;

public abstract class ProblemBuildingException extends RuntimeException  {
    public ProblemBuildingException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();

    public Problem buildReport() {
        return Builders.problem()
                .status(getStatus())
                .title(getMessage())
                .errorClass(getClass().getSimpleName())
                .build();
    }
}
