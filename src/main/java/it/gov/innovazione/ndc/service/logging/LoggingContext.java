package it.gov.innovazione.ndc.service.logging;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.logging.LogLevel;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder(toBuilder = true)
@With
@Data
public class LoggingContext {
    private static final Map<Integer, Pair<String, Function<LoggingContext, Object>>> contextMapper =
            Map.of(
                    2, Pair.of("RepoUrl", LoggingContext::getRepoUrl),
                    3, Pair.of("Path", LoggingContext::getPath),
                    4, Pair.of("MainResource", LoggingContext::getMainResource),
                    5, Pair.of("Message", LoggingContext::getMessage),
                    6, Pair.of("Status", LoggingContext::getHarvesterStatus),
                    7, Pair.of("Details", LoggingContext::getDetails),
                    8, Pair.of("EventCategory", LoggingContext::getEventCategory));
    @Builder.Default
    private final LogLevel level = LogLevel.INFO;
    @Builder.Default
    private final String component = "HARVESTER";
    private final String jobId;
    private final String repoUrl;
    private final String path;
    private final String mainResource;
    private final String message;
    private final String details;
    @Singular
    private final Map<String, Object> additionalInfos;
    private final HarvesterRun.Status harvesterStatus;
    private final HarvesterStage stage;
    private final EventCategory eventCategory;

    String makeLogEntry() {
        String logMessage =
                Stream.concat(contextMapper.entrySet().stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .map(Map.Entry::getValue)
                                        .map(pair -> Pair.of(pair.getKey(), pair.getRight().apply(this))),
                                additionalInfos.entrySet().stream())
                        .filter(pair -> Objects.nonNull(pair.getValue()))
                        .filter(pair -> StringUtils.isNoneBlank(pair.getValue().toString()))
                        .map(e -> e.getKey() + ": " + e.getValue())
                        .collect(Collectors.joining(" "));

        String logHeaders =
                Stream.of(component,
                                stage.name(),
                                jobId)
                        .map(s -> Objects.isNull(s) ? "" : s)
                        .map(String::toUpperCase)
                        .map(String::trim)
                        .map(s -> "[" + s + "]")
                        .collect(Collectors.joining(" "));

        return logHeaders + " " + logMessage;
    }

    public LoggingContext semantic() {
        return this.withEventCategory(EventCategory.SEMANTIC);
    }

    public LoggingContext infrastructure() {
        return this.withEventCategory(EventCategory.INFRASTRUCTURE);
    }

    public LoggingContext warn() {
        return this.withLevel(LogLevel.WARN);
    }

    public LoggingContext trace() {
        return this.withLevel(LogLevel.TRACE);
    }

    public LoggingContext error() {
        return this.withLevel(LogLevel.ERROR);
    }

}
