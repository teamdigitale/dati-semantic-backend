package it.gov.innovazione.ndc.harvester.csv;

public class InvalidCsvException extends RuntimeException {
    public InvalidCsvException(String message) {
        super(message);
    }

    public InvalidCsvException(String message, Throwable cause) {
        super(message, cause);
    }
}
