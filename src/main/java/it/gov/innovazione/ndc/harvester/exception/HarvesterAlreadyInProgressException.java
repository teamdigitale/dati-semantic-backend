package it.gov.innovazione.ndc.harvester.exception;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;

public class HarvesterAlreadyInProgressException extends HarvesterException {
    public HarvesterAlreadyInProgressException(String format) {
        super(format);
    }

    @Override
    public HarvesterRun.Status getHarvesterRunStatus() {
        return HarvesterRun.Status.ALREADY_RUNNING;
    }
}
