package it.gov.innovazione.ndc.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticAssetStats {
  private final SemanticAssetTypeStats total;
  private final SemanticAssetTypeStats controlledVocabulary;
  private final SemanticAssetTypeStats ontology;
  private final SemanticAssetTypeStats schema;

  private static double round(double value) {
    BigDecimal bigDecimal = BigDecimal.valueOf(value);
    return bigDecimal.setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

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
      return round(lastYear == 0 ? 0 : ((double) current - lastYear) / lastYear * 100);
    }
  }

  @Builder
  public static class StatusStat {
    private final double archived;
    private final double published;
    private final double closedAccess;
    private final double draft;
    private final double unknown;

    public double getArchived() {
      return round(archived * 100);
    }

    public double getPublished() {
      return round(published * 100);
    }

    public double getClosedAccess() {
      return round(closedAccess * 100);
    }

    public double getDraft() {
      return round(draft * 100);
    }

    public double getUnknown() {
      return round(unknown * 100);
    }
  }
}
