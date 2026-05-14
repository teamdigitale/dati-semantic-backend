package it.gov.innovazione.ndc.controller.audit;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResourceDeltaPage {
    private final RunInfo run;
    private final List<ResourceDeltaItem> content;
    private final int offset;
    private final int limit;
    private final int total;
}
