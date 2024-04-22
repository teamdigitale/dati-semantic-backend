package it.gov.innovazione.ndc.harvester.exception;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;

public abstract class HarvesterException extends RuntimeException {

    HarvesterException(String message) {
        super(message);
    }

    public abstract HarvesterRun.Status getHarvesterRunStatus();
}
