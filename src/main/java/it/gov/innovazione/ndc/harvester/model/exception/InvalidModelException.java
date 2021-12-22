package it.gov.innovazione.ndc.harvester.model.exception;

public class InvalidModelException extends RuntimeException {
    public InvalidModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidModelException(String message) {
        super(message);
    }
}
