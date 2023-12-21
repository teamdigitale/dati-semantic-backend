package it.gov.innovazione.ndc.harvester;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class JobExecutionResponse {
    private final String runId;
    private final String correlationId;
    private final String repositoryId;
    private final String repositoryUrl;
    private final String startedAt;
    private final boolean forced;
}
