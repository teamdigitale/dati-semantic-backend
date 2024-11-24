package it.gov.innovazione.ndc.harvester.context;

import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class HarvestExecutionContextUtils {

    private static final ThreadLocal<HarvestExecutionContext> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<List<SemanticContentStats>> SEMANTIC_CONTENT_STATS_HOLDER = ThreadLocal.withInitial(ArrayList::new);

    public static HarvestExecutionContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void setContext(HarvestExecutionContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
        clearSemanticContentStats();
    }

    public static List<SemanticContentStats> getSemanticContentStats() {
        return SEMANTIC_CONTENT_STATS_HOLDER.get();
    }

    public static void addSemanticContentStat(SemanticContentStats stats) {
        SEMANTIC_CONTENT_STATS_HOLDER.get().add(stats);
    }

    public static void clearSemanticContentStats() {
        SEMANTIC_CONTENT_STATS_HOLDER.get().clear();
    }
}
