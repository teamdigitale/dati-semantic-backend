package it.gov.innovazione.ndc.controller;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.List;

@With
@Builder(toBuilder = true)
@Data
public class AggregateDashboardResponse {
    private final List<String> headers;
    private final List<List<Object>> rows;
}
