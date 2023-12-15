package it.gov.innovazione.ndc.harvester;

import lombok.Builder;
import lombok.Data;
import org.springframework.batch.core.JobParameter;

import java.util.Map;

@Builder
@Data
public class JobExecutionResponse {
    private final String jobId;
    private final String jobInstanceId;
    private final Map<String, JobParameter> jobParameters;
    private final String correlationId;
    private final String repositoryId;
    private final String repositoryUrl;
    private final String startedAt;
    private final boolean forced;
    private final ExecutionStatus status;

    public enum ExecutionStatus {
        STARTED,
        UNCHANGED,
        FAILURE
    }

}
