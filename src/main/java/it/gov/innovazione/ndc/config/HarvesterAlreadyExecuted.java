package it.gov.innovazione.ndc.config;

public class HarvesterAlreadyExecuted extends RuntimeException {
    public HarvesterAlreadyExecuted(String format) {
        super(format);
    }
}
