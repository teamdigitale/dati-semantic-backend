package it.gov.innovazione.ndc.model.harvester;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HarvesterRunResult {
    private final int totalCount;
    private final int limit;
    private final int offset;
    private final List<HarvesterRun> data;
}