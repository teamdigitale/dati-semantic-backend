package it.teamdigitale.ndc.controller.exception;

import org.springframework.http.HttpStatus;

public abstract class AppProblemGeneratingException extends RuntimeException  {
    public AppProblemGeneratingException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();

    public ApplicationProblem buildReport() {
        return ApplicationProblem.builder()
                .status(getStatus().value())
                .title(getMessage())
                .type(getTypeUri())
                .build();
    }

    protected String getTypeUri() {
        return ApplicationProblem.getErrorUri(getClass().getSimpleName());
    }

}
