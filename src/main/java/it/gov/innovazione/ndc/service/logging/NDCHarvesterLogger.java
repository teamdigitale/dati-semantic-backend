package it.gov.innovazione.ndc.service.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;

import java.io.IOException;
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
        LoggingContext context = NDCHarvesterLoggerUtils.getContext();
        if (context != null) {
            loggingContext = NDCHarvesterLoggerUtils.mergeContexts(context, loggingContext);
        }
        try {
            Consumer<String> logMethod = mapper.get(loggingContext.getLevel());
            String message = loggingContext.makeLogEntry();
            logIfNecessary(logMethod, message);
        } catch (Exception e) {
            log.error("There was an exception while logging", e);
        }
    }

    private static void logIfNecessary(Consumer<String> logMethod, String message) {
        if (NDCHarvesterLoggerUtils.notSeen(message)) {
            logMethod.accept(message);
        }
    }

    public static void logSemanticInfo(LoggingContext loggingContext) {
        log(loggingContext.semantic().withLevel(LogLevel.INFO));
    }

    public static void logInfrastructureInfo(LoggingContext loggingContext) {
        log(loggingContext.infrastructure().withLevel(LogLevel.INFO));
    }

    public static void logSemanticError(LoggingContext loggingContext) {
        log(loggingContext.error().semantic());
    }

    public static void logInfrastructureError(LoggingContext loggingContext) {
        log(loggingContext.error().infrastructure());
    }

    public static void logSemanticWarn(LoggingContext loggingContext) {
        log(loggingContext.warn().semantic());
    }

    public static void logSemanticTrace(LoggingContext loggingContext) {
        log(loggingContext.trace().semantic());
    }

    public static void logApplicationInfo(LoggingContext loggingContext) {
        log(loggingContext.withLevel(LogLevel.INFO).application());

    }

    public static void logApplicationWarn(LoggingContext build) {
        log(build.withLevel(LogLevel.WARN).application());
    }

    public static void logApplicationError(LoggingContext build) {
        log(build.withLevel(LogLevel.ERROR).application());
    }
}
