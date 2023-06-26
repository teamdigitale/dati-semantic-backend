package it.gov.innovazione.ndc.controller.exception;

import org.springframework.http.HttpStatus;

public class InvalidSemanticAssetException extends ProblemBuildingException {

    public InvalidSemanticAssetException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

}
