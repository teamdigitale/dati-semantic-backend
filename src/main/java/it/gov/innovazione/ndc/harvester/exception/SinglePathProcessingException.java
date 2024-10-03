package it.gov.innovazione.ndc.harvester.exception;

import lombok.Getter;

import java.util.Optional;

public class SinglePathProcessingException extends RuntimeException {

    @Getter
    private final boolean isFatal;

    public SinglePathProcessingException(String message, boolean isFatal) {
        super(message);
        this.isFatal = isFatal;
    }

    public SinglePathProcessingException(String message, Throwable cause, boolean isFatal) {
        super(message, cause);
        this.isFatal = isFatal;
    }

    public String getRealErrorMessage() {
        return Optional.ofNullable(this.getCause())
                .map(Throwable::getMessage)
                .orElse(this.getMessage());
    }
}
