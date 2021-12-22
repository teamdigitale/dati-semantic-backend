package it.gov.innovazione.ndc.harvester.csv;

import java.util.List;

public interface HeadersToIdNameExtractor {
    String extract(List<String> headerNames);
}
