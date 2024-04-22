package it.gov.innovazione.ndc.harvester.exception;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;

public class HarvesterAlreadyExecuted extends HarvesterException {
    public HarvesterAlreadyExecuted(String format) {
        super(format);
    }

    @Override
    public HarvesterRun.Status getHarvesterRunStatus() {
        return HarvesterRun.Status.UNCHANGED;
    }
}
