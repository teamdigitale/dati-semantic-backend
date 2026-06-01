package it.gov.innovazione.ndc.controller.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SemanticAssetChangelogPage {
    private final String assetIri;
    private final SemanticAssetType assetType;
    private final List<ChangelogEntry> content;
    private final int offset;
    private final int limit;
    private final int total;
}
