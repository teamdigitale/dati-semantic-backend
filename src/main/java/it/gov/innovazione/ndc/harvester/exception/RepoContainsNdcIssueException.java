package it.gov.innovazione.ndc.harvester.exception;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;

public class RepoContainsNdcIssueException extends HarvesterException {
    public RepoContainsNdcIssueException(String message) {
        super(message);
    }

    @Override
    public HarvesterRun.Status getHarvesterRunStatus() {
        return HarvesterRun.Status.NDC_ISSUES_PRESENT;
    }
}
