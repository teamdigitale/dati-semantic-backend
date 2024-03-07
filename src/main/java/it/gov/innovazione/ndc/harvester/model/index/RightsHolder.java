package it.gov.innovazione.ndc.harvester.model.index;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RightsHolder {
    private String identifier;
    private Map<String, String> name;
}
