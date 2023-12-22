package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RunningInstance {
    private final String threadName;
    private final HarvesterRun harvesterRun;
}
