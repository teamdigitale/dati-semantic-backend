package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticAssetStatSample {
  private final String resourceUri;
  private final SemanticAssetType resourceType;
  private final String rightHolder;
  private final boolean hasErrors;
  private final boolean hasWarnings;
  private final String statusType;
  private final int yearOfHarvest;
}
