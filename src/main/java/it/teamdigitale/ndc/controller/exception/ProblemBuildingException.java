package it.teamdigitale.ndc.controller.exception;

import it.teamdigitale.ndc.gen.dto.Problem;
import it.teamdigitale.ndc.model.Builders;
import org.springframework.http.HttpStatus;

public abstract class ProblemBuildingException extends RuntimeException  {
    public ProblemBuildingException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();

    public Problem buildReport() {
        return Builders.problem()
                .status(getStatus().value())
                .title(getMessage())
                .errorClass(getClass().getSimpleName())
                .build();
    }
}
