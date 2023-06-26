package it.gov.innovazione.ndc.controller.exception;

import org.springframework.http.HttpStatus;

public class SemanticAssetGenericErrorException extends ProblemBuildingException {

    public SemanticAssetGenericErrorException() {
        super("Error during file validation");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
