package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

@With
@Data
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HarvestExecutionContext {
    private final Repository repository;
    private final String revision;
    private final String correlationId;
    private final String runId;
    private final String currentUserId;
    private final String rootPath;
}
