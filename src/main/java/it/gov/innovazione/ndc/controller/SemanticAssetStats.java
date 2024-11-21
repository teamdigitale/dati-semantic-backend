package it.gov.innovazione.ndc.controller;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticAssetStats {
    private final SemanticAssetTypeStats totalStats;
    private final SemanticAssetTypeStats controlledVocabularyStats;
    private final SemanticAssetTypeStats ontologyStats;
    private final SemanticAssetTypeStats schemaStats;

    @Data
    @Builder
    public static class SemanticAssetTypeStats {
        private final long current;
        private final long lastYear;

        public long getIncrementOverLastYear() {
            return current - lastYear;
        }

        public double getIncrementPercentageOverLastYear() {
            return lastYear == 0 ? 0 : ((double) current - lastYear) / lastYear * 100;
        }
    }
}
