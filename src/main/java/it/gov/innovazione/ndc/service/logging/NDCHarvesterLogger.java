package it.gov.innovazione.ndc.service.logging;

import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class NDCHarvesterLogger {

    private static final Map<LogLevel, Consumer<String>> mapper = Map.of(
            LogLevel.ERROR, log::error,
            LogLevel.WARN, log::warn,
            LogLevel.INFO, log::info,
            LogLevel.DEBUG, log::debug,
            LogLevel.TRACE, log::trace
    );

    private static void log(LoggingContext loggingContext) {
        HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
        if (context != null) {
            loggingContext = loggingContext.toBuilder()
                    .additionalInfo("revision", context.getRevision())
                    .additionalInfo("correlationId", context.getCorrelationId())
                    .additionalInfo("currentUserId", context.getCurrentUserId())
                    .additionalInfo("rootPath", context.getRootPath())
                    .additionalInfo("instance", context.getInstance())
                    .build()
                    .withJobId(context.getRunId())
                    .withRepoUrl(context.getRepository().getUrl());
        }
        try {
            Consumer<String> logMethod = mapper.get(loggingContext.getLevel());
            logMethod.accept(loggingContext.makeLogEntry());
        } catch (Exception e) {
            log.error("There was an exception while logging", e);
        }
    }

    public static void logSemanticInfo(LoggingContext loggingContext) {
        log(loggingContext.semantic().withLevel(LogLevel.INFO));
    }

    public static void logSemanticError(LoggingContext loggingContext) {
        log(loggingContext.error().semantic());
    }

    public static void logSemanticWarn(LoggingContext loggingContext) {
        log(loggingContext.warn().semantic());
    }

    public static void logSemanticTrace(LoggingContext loggingContext) {
        log(loggingContext.trace().semantic());
    }

}
