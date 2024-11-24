package it.gov.innovazione.ndc.controller;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
            BigDecimal bigDecimal = BigDecimal.valueOf(
                    lastYear == 0
                            ? 0
                            : ((double) current - lastYear) / lastYear * 100);
            return bigDecimal.setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
    }
}
