package it.teamdigitale.ndc.harvester.util;

public class GitRepoCloneException extends RuntimeException {
    public GitRepoCloneException(String message, Throwable cause) {
        super(message, cause);
    }
}
