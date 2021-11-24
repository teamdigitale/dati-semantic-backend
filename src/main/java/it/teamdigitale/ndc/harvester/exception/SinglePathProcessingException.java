package it.teamdigitale.ndc.harvester.exception;

public class SinglePathProcessingException extends RuntimeException {
    public SinglePathProcessingException(String message) {
        super(message);
    }

    public SinglePathProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
