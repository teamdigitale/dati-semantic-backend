package it.gov.innovazione.ndc.service.logging;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
public class NDCHarvesterLoggerUtils {

    private static final ThreadLocal<LoggingContext> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> SEEN_MESSAGES = ThreadLocal.withInitial(HashSet::new);

    public static LoggingContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void setInitialContext(LoggingContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void overrideContext(LoggingContext context) {
        CONTEXT_HOLDER.set(NDCHarvesterLoggerUtils.mergeContexts(
                CONTEXT_HOLDER.get(),
                context));
    }

    public static void clearContext() {
        SEEN_MESSAGES.get().clear();
        CONTEXT_HOLDER.remove();
        log.info("Contexts cleared");
    }

    public static boolean notSeen(String message) {
        return SEEN_MESSAGES.get().add(message);
    }

    public static LoggingContext mergeContexts(LoggingContext context, LoggingContext additionalContext) {
        LoggingContext.LoggingContextBuilder builder = Optional.ofNullable(context)
                .map(LoggingContext::toBuilder)
                .orElseGet(LoggingContext::builder);
        if (additionalContext != null) {
            builder = addToBuilder(additionalContext, builder, LoggingContext::getLevel, LoggingContext.LoggingContextBuilder::level);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getComponent, LoggingContext.LoggingContextBuilder::component);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getJobId, LoggingContext.LoggingContextBuilder::jobId);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getRepoUrl, LoggingContext.LoggingContextBuilder::repoUrl);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getPath, LoggingContext.LoggingContextBuilder::path);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getMainResource, LoggingContext.LoggingContextBuilder::mainResource);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getMessage, LoggingContext.LoggingContextBuilder::message);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getDetails, LoggingContext.LoggingContextBuilder::details);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getHarvesterStatus, LoggingContext.LoggingContextBuilder::harvesterStatus);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getStage, LoggingContext.LoggingContextBuilder::stage);
            builder = addToBuilder(additionalContext, builder, LoggingContext::getEventCategory, LoggingContext.LoggingContextBuilder::eventCategory);
            for (Map.Entry<String, Object> entry : additionalContext.getAdditionalInfos().entrySet()) {
                if (entry.getValue() != null) {
                    builder = builder.additionalInfo(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.build();
    }

    private static <T> LoggingContext.LoggingContextBuilder addToBuilder(
            LoggingContext additionalContext,
            LoggingContext.LoggingContextBuilder builder,
            Function<LoggingContext, T> getter,
            BiFunction<LoggingContext.LoggingContextBuilder, T, LoggingContext.LoggingContextBuilder> setter) {
        return Optional.of(additionalContext)
                .map(getter)
                .map(value -> setter.apply(builder, value))
                .orElse(builder);
    }
}
