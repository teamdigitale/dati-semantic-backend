package it.gov.innovazione.ndc.harvester.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.model.HarvesterStatsHolder;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticContentStatsService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void saveStats() {
        List<SemanticContentStats> semanticContentStats = HarvestExecutionContextUtils.getSemanticContentStats();
        semanticContentStats.forEach(this::save);
        HarvestExecutionContextUtils.clearSemanticContentStats();
    }

    public int save(SemanticContentStats semanticContentStats) {
        String statement = "INSERT INTO SEMANTIC_CONTENT_STATS ("
                + "ID, "
                + "HARVESTER_RUN_ID, "
                + "RESOURCE_URI, "
                + "RESOURCE_TYPE, "
                + "RIGHT_HOLDER, "
                + "ISSUED_ON, "
                + "MODIFIED_ON, "
                + "HAS_ERRORS,"
                + "HAS_WARNINGS, "
                + "STATUS ) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(statement,
                UUID.randomUUID().toString(),
                semanticContentStats.getHarvesterRunId(),
                semanticContentStats.getResourceUri(),
                semanticContentStats.getResourceType().name(),
                semanticContentStats.getRightHolder(),
                semanticContentStats.getIssuedOn(),
                semanticContentStats.getModifiedOn(),
                semanticContentStats.isHasErrors(),
                semanticContentStats.isHasWarnings(),
                toJsonArray(semanticContentStats.getStatus()));
    }

    private String toJsonArray(List<String> status) {
        try {
            return objectMapper.writeValueAsString(CollectionUtils.emptyIfNull(status));
        } catch (Exception e) {
            NDCHarvesterLogger.logApplicationError(LoggingContext.builder()
                    .message("Error converting status to json array")
                    .details(e.getMessage())
                    .additionalInfo("status", status)
                    .build());
            log.error("Error converting status to json", e);
            return "[]";
        }
    }

    public void updateStats(HarvesterStatsHolder harvesterStatsHolder) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (Objects.nonNull(context)) {
            SemanticContentStats stats = SemanticContentStats.builder()
                    .harvesterRunId(context.getRunId())
                    .resourceUri(harvesterStatsHolder.getMetadata().getIri())
                    .hasErrors(harvesterStatsHolder.getValidationContextStats().getErrors() > 0)
                    .hasWarnings(harvesterStatsHolder.getValidationContextStats().getWarnings() > 0)
                    .status(harvesterStatsHolder.getMetadata().getStatus())
                    .rightHolder(harvesterStatsHolder.getMetadata().getAgencyId())
                    .issuedOn(harvesterStatsHolder.getMetadata().getIssuedOn())
                    .modifiedOn(harvesterStatsHolder.getMetadata().getModifiedOn())
                    .resourceType(harvesterStatsHolder.getMetadata().getType())
                    .build();
            HarvestExecutionContextUtils.addSemanticContentStat(stats);
        }
    }
}
