package it.gov.innovazione.ndc.harvester.exception;

import java.util.Optional;

public class SinglePathProcessingException extends RuntimeException {
    public SinglePathProcessingException(String message) {
        super(message);
    }

    public SinglePathProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getRealErrorMessage() {
        return Optional.ofNullable(this.getCause())
                .map(Throwable::getMessage)
                .orElse(this.getMessage());
    }
}
