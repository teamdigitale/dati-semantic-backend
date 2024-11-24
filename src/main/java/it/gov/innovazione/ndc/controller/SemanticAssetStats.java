package it.gov.innovazione.ndc.controller;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
public class SemanticAssetStats {
    private final SemanticAssetTypeStats total;
    private final SemanticAssetTypeStats controlledVocabulary;
    private final SemanticAssetTypeStats ontology;
    private final SemanticAssetTypeStats schema;

    @Data
    @Builder
    public static class SemanticAssetTypeStats {
        private final long current;
        private final long lastYear;
        private final StatusStat status;

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

    @Data
    @Builder
    public static class StatusStat {
        private final double archived;
        private final double published;
        private final double closedAccess;
        private final double draft;
    }
}
