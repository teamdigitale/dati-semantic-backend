package it.gov.innovazione.ndc.service.logging;

public enum HarvesterStage {
    START,
    CLONE_REPO,
    MAINTAINER_EXTRACTION,
    CLEANING_VIRTUOSO,
    CLEANING_METADATA,
    PROCESS_RESOURCE
}
