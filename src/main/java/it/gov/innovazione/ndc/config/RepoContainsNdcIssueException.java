package it.gov.innovazione.ndc.config;

public class RepoContainsNdcIssueException extends RuntimeException {
    public RepoContainsNdcIssueException(String message) {
        super(message);
    }
}
