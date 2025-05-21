package it.gov.innovazione.ndc.model.harvester;

import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.time.Instant;

@Data
@Builder
@RequiredArgsConstructor
public class HarvesterRun {
    private final String id;
    private final String correlationId;
    private final String repositoryId;
    private final String repositoryUrl;
    private final String instance;
    private final Instant startedAt;
    private final String startedBy;
    private final Instant endedAt;
    private final String revision;
    @With
    private final Instant revisionCommittedAt;
    private final Status status;
    private final String reason;

    public static String getCurrentRunId() {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (context == null) {
            return null;
        }
        return context.getRunId();
    }

    public boolean hasDatesSet() {
        return startedAt != null && endedAt != null;
    }

    public enum Status {
        SUCCESS, UNCHANGED, ALREADY_RUNNING, RUNNING, NDC_ISSUES_PRESENT, FAILURE
    }

}
