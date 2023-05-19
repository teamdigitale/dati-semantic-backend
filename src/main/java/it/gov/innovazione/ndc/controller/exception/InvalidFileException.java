package it.gov.innovazione.ndc.controller.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends ProblemBuildingException {

    public InvalidFileException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
