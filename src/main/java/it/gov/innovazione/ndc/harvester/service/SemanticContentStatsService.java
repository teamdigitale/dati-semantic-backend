package it.gov.innovazione.ndc.harvester.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.controller.SemanticAssetStatSample;
import it.gov.innovazione.ndc.controller.SemanticAssetStats;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.model.HarvesterStatsHolder;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticContentStatsService {

  public static final String HAS_STATS_QUERY =
      """
            select 1
            where exists (select 1
                          from SEMANTIC_CONTENT_STATS SCS
                                   JOIN LATEST_HARVESTER_RUN_BY_YEAR LHRBY on SCS.HARVESTER_RUN_ID = LHRBY.ID
                          where YEAR(LHRBY.STARTED) = ?);
            """;
  public static final String GET_STATS_QUERY =
      """
              select SCS.RESOURCE_URI,
                     SCS.RESOURCE_TYPE,
                     SCS.RIGHT_HOLDER,
                     SCS.HAS_ERRORS,
                     SCS.HAS_WARNINGS,
                     CASE
                         WHEN JSON_CONTAINS(lower(SCS.STATUS), '\\"archived\\"') THEN 'Archiviato'
                         WHEN JSON_CONTAINS(lower(SCS.STATUS), '\\"catalogued\\"') || JSON_CONTAINS(lower(SCS.STATUS), '\\"published\\"') THEN 'Stabile'
                         WHEN JSON_CONTAINS(lower(SCS.STATUS), '\\"closed access\\"') THEN 'Accesso Ristretto'
                         WHEN JSON_CONTAINS(lower(SCS.STATUS), '\\"initial draft\\"') ||
                              JSON_CONTAINS(lower(SCS.STATUS), '\\"draft\\"') ||
                              JSON_CONTAINS(lower(SCS.STATUS), '\\"final draft\\"') ||
                              JSON_CONTAINS(lower(SCS.STATUS), '\\"intermediate draft\\"') ||
                              JSON_CONTAINS(lower(SCS.STATUS), '\\"submitted\\"')
                              THEN 'Bozza'
                         ELSE 'unknown'
                         END          as STATUS_TYPE,
                     YEAR(LHRBY.STARTED) as YEAR_OF_HARVEST
              from SEMANTIC_CONTENT_STATS SCS
                       JOIN LATEST_HARVESTER_RUN_BY_YEAR LHRBY on SCS.HARVESTER_RUN_ID = LHRBY.ID
              where YEAR(LHRBY.STARTED) = ? OR YEAR(LHRBY.STARTED) = ?;
              """;
  public static final RowMapper<SemanticAssetStatSample> SEMANTIC_ASSET_STAT_SAMPLE_ROW_MAPPER =
      (rs, rowNum) ->
          SemanticAssetStatSample.builder()
              .resourceUri(rs.getString("RESOURCE_URI"))
              .resourceType(SemanticAssetType.valueOf(rs.getString("RESOURCE_TYPE")))
              .rightHolder(rs.getString("RIGHT_HOLDER"))
              .hasErrors(rs.getBoolean("HAS_ERRORS"))
              .hasWarnings(rs.getBoolean("HAS_WARNINGS"))
              .statusType(rs.getString("STATUS_TYPE"))
              .yearOfHarvest(rs.getInt("YEAR_OF_HARVEST"))
              .build();
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public void saveStats() {
    List<SemanticContentStats> semanticContentStats =
        HarvestExecutionContextUtils.getSemanticContentStats();
    semanticContentStats.forEach(this::save);
    HarvestExecutionContextUtils.clearSemanticContentStats();
  }

  public int save(SemanticContentStats semanticContentStats) {
    String statement =
        "INSERT INTO SEMANTIC_CONTENT_STATS ("
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
    return jdbcTemplate.update(
        statement,
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
      NDCHarvesterLogger.logApplicationError(
          LoggingContext.builder()
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
      SemanticContentStats stats =
          SemanticContentStats.builder()
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

  public boolean hasStats(int year) {
    Boolean result = jdbcTemplate.query(HAS_STATS_QUERY, ResultSet::next, year);
    return Optional.ofNullable(result).orElse(false);
  }

  public SemanticAssetStats getStats(int year) {
    List<SemanticAssetStatSample> samples =
        jdbcTemplate.query(GET_STATS_QUERY, SEMANTIC_ASSET_STAT_SAMPLE_ROW_MAPPER, year, year - 1);

    boolean hasLastYear =
        samples.stream().anyMatch(sample -> sample.getYearOfHarvest() == year - 1);

    int lastYear = year - 1;
    if (!hasLastYear) {
      lastYear = year;
    }

    return SemanticAssetStats.builder()
        .total(getSemanticAssetTypeStats(samples, year, lastYear, sample -> true))
        .controlledVocabulary(
            getSemanticAssetTypeStats(
                samples,
                year,
                lastYear,
                sample -> sample.getResourceType() == SemanticAssetType.CONTROLLED_VOCABULARY))
        .ontology(
            getSemanticAssetTypeStats(
                samples,
                year,
                lastYear,
                sample -> sample.getResourceType() == SemanticAssetType.ONTOLOGY))
        .schema(
            getSemanticAssetTypeStats(
                samples,
                year,
                lastYear,
                sample -> sample.getResourceType() == SemanticAssetType.SCHEMA))
        .build();
  }

  private SemanticAssetStats.SemanticAssetTypeStats getSemanticAssetTypeStats(
      List<SemanticAssetStatSample> samples,
      int year,
      int lastYear,
      Predicate<SemanticAssetStatSample> filter) {
    int totalCurrent =
        (int)
            samples.stream()
                .filter(sample -> sample.getYearOfHarvest() == year)
                .filter(filter)
                .count();
    int totalLastYear =
        (int)
            samples.stream()
                .filter(sample -> sample.getYearOfHarvest() == lastYear)
                .filter(filter)
                .count();
    Map<String, List<SemanticAssetStatSample>> byStatus =
        samples.stream()
            .filter(sample -> sample.getYearOfHarvest() == year)
            .filter(filter)
            .collect(Collectors.groupingBy(SemanticAssetStatSample::getStatusType));
    return SemanticAssetStats.SemanticAssetTypeStats.builder()
        .current(totalCurrent)
        .lastYear(totalLastYear)
        .status(
            SemanticAssetStats.StatusStat.builder()
                .archived(
                    totalCurrent > 0
                        ? byStatus.getOrDefault("Archiviato", List.of()).stream()
                                .filter(filter)
                                .count()
                            / (double) totalCurrent
                        : 0)
                .published(
                    totalCurrent > 0
                        ? byStatus.getOrDefault("Stabile", List.of()).stream()
                                .filter(filter)
                                .count()
                            / (double) totalCurrent
                        : 0)
                .closedAccess(
                    totalCurrent > 0
                        ? byStatus.getOrDefault("Accesso Ristretto", List.of()).stream()
                                .filter(filter)
                                .count()
                            / (double) totalCurrent
                        : 0)
                .draft(
                    totalCurrent > 0
                        ? byStatus.getOrDefault("Bozza", List.of()).stream().filter(filter).count()
                            / (double) totalCurrent
                        : 0)
                .unknown(
                    totalCurrent > 0
                        ? byStatus.getOrDefault("unknown", List.of()).stream()
                                .filter(filter)
                                .count()
                            / (double) totalCurrent
                        : 0)
                .build())
        .build();
  }
}
