package it.gov.innovazione.ndc.harvester.exception;

public class InvalidAssetException extends SinglePathProcessingException {
    public InvalidAssetException(String message) {
        super(message, false);
    }
}
