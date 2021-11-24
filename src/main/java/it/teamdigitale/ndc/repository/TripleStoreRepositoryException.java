package it.teamdigitale.ndc.repository;

public class TripleStoreRepositoryException extends RuntimeException {
    public TripleStoreRepositoryException(String message) {
        super(message);
    }

    public TripleStoreRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
