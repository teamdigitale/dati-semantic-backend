package it.gov.innovazione.ndc.config;

public class HarvesterAlreadyInProgress extends RuntimeException {
    public HarvesterAlreadyInProgress(String format) {
        super(format);
    }
}
