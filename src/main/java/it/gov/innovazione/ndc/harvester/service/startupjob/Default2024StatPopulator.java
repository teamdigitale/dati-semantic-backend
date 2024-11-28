package it.gov.innovazione.ndc.harvester.service.startupjob;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.service.SemanticContentStatsService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLoggerUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Default2024StatPopulator implements StartupJob {

    private static final int DEFAULT_YEAR = 2024;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final RepositoryService repositoryService;
    private final HarvesterRunService harvesterRunService;
    private final SemanticContentStatsService semanticContentStatsService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Map<String, HarvesterRun> latestHarvesterRunByRepoUrl;

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {

            NDCHarvesterLoggerUtils.setInitialContext(LoggingContext.builder()
                    .component("Default2024StatPopulator")
                    .build());

            if (defaultDataLoadIsNotNeeded()) {
                NDCHarvesterLogger.logApplicationInfo(LoggingContext.builder()
                        .message("Default 2024 stats already loaded")
                        .build());
                return;
            }

            Resource resource = resourceLoader.getResource("classpath:db/default-2024-stats.json");
            List<Map<String, Object>> default2024StatsPath = objectMapper.readValue(resource.getContentAsString(Charset.defaultCharset()), List.class);
            List<String> activeRepos = repositoryService.getActiveRepos().stream()
                    .map(Repository::getUrl)
                    .toList();

            latestHarvesterRunByRepoUrl = harvesterRunService.getAllRuns().stream()
                    .filter(harvesterRun -> activeRepos.contains(harvesterRun.getRepositoryUrl()))
                    .filter(harvesterRun -> LocalDateTime.ofInstant(harvesterRun.getStartedAt(), ZoneId.systemDefault()).getYear() == DEFAULT_YEAR)
                    .collect(Collectors.groupingBy(HarvesterRun::getRepositoryUrl))
                    .entrySet().stream()
                    .map(stringListEntry -> Pair.of(
                            stringListEntry.getKey(),
                            stringListEntry.getValue().stream().min(Comparator.comparing(HarvesterRun::getStartedAt)).orElse(null)))
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

            default2024StatsPath.forEach(this::insertIfPossible);

            log.info("Default 2024 stats loaded");
        } catch (RuntimeException | IOException e) {
            NDCHarvesterLogger.logApplicationError(LoggingContext.builder()
                    .message("Error while populating default 2024 stats")
                    .details(e.getMessage())
                    .build());
            log.error("Error while reading default 2024 stats", e);
        }
    }

    private boolean defaultDataLoadIsNotNeeded() {
        return semanticContentStatsService.hasStats(DEFAULT_YEAR);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private void insertIfPossible(Map<String, Object> stat) {
        try {
            if (latestHarvesterRunByRepoUrl.containsKey(stat.get("REPOSITORY_URL").toString())) {
                String repoUrl = stat.get("REPOSITORY_URL").toString();
                HarvesterRun harvesterRun = latestHarvesterRunByRepoUrl.get(repoUrl);
                if (harvesterRun != null) {
                    String harvesterRunId = harvesterRun.getId();
                    SemanticContentStats semanticContentStats = SemanticContentStats.builder()
                            .resourceUri(stat.get("RESOURCE_URI").toString())
                            .harvesterRunId(harvesterRunId)
                            .resourceType(SemanticAssetType.valueOf(stat.get("RESOURCE_TYPE").toString()))
                            .rightHolder(stat.get("RIGHT_HOLDER").toString())
                            .issuedOn(Optional.ofNullable(stat.get("ISSUED_ON"))
                                    .map(o -> LocalDateTime.parse(o.toString(), dateTimeFormatter).toLocalDate())
                                    .orElse(null))
                            .modifiedOn(
                                    Optional.ofNullable(stat.get("MODIFIED_ON"))
                                            .map(o -> LocalDateTime.parse(o.toString(), dateTimeFormatter).toLocalDate())
                                            .orElse(null))
                            .hasErrors(stat.get("HAS_ERRORS").toString().equals("1"))
                            .hasWarnings(stat.get("HAS_WARNINGS").toString().equals("1"))
                            .status(objectMapper.readValue(stat.get("STATUS").toString(), List.class))
                            .build();
                    semanticContentStatsService.save(semanticContentStats);
                }
            } else {
                NDCHarvesterLogger.logApplicationWarn(LoggingContext.builder()
                        .message("Cannot insert default 2024 stats")
                        .details("No harvester run found for repository " + stat.get("REPOSITORY_URL"))
                        .build());
                log.warn("Cannot insert default 2024 stats for " + stat.get("RESOURCE_URI") + ". No harvester run found for repository " + stat.get("REPOSITORY_URL"));
            }
        } catch (RuntimeException e) {
            NDCHarvesterLogger.logApplicationError(LoggingContext.builder()
                    .message("Error while inserting default 2024 stats for " + stat.get("RESOURCE_URI"))
                    .details(e.getMessage())
                    .build());
            log.error("Error while inserting default " + DEFAULT_YEAR + " stats", e);
        }
    }
}
