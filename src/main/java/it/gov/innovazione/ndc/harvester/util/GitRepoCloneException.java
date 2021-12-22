package it.gov.innovazione.ndc.harvester.util;

public class GitRepoCloneException extends RuntimeException {
    public GitRepoCloneException(String message, Throwable cause) {
        super(message, cause);
    }
}
