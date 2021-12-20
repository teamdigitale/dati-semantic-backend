package it.teamdigitale.ndc.harvester.csv;

import java.util.List;

public interface HeadersToIdNameExtractor {
    String extract(List<String> headerNames);
}
