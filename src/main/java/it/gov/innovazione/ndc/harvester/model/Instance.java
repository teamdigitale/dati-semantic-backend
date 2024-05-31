package it.gov.innovazione.ndc.harvester.model;

public enum Instance {
    PRIMARY,
    SECONDARY;

    public Instance switchInstance() {
        if (this == PRIMARY) {
            return SECONDARY;
        } else {
            return PRIMARY;
        }
    }
}
