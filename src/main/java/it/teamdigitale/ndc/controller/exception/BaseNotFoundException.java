package it.teamdigitale.ndc.controller.exception;

import org.springframework.http.HttpStatus;

public class BaseNotFoundException extends ProblemBuildingException {
    public BaseNotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
