package it.teamdigitale.ndc.harvester.model.index;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeSummary {

    private String iri;
    private String summary;
}
