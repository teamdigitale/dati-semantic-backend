package it.gov.innovazione.ndc.controller.audit;

import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RunInfo {
    private final String id;
    private final String repositoryId;
    private final String revision;
    private final Instant revisionCommittedAt;
    private final Instant startedAt;
    private final Instant endedAt;
    private final HarvesterRun.Status status;

    public static RunInfo of(HarvesterRun run) {
        if (run == null) {
            return null;
        }
        return RunInfo.builder()
                .id(run.getId())
                .repositoryId(run.getRepositoryId())
                .revision(run.getRevision())
                .revisionCommittedAt(run.getRevisionCommittedAt())
                .startedAt(run.getStartedAt())
                .endedAt(run.getEndedAt())
                .status(run.getStatus())
                .build();
    }
}
