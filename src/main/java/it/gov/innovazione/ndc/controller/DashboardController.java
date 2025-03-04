package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.controller.date.DateParameter.Granularity;
import it.gov.innovazione.ndc.service.DashboardService;
import it.gov.innovazione.ndc.service.DashboardService.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@RestController
@RequestMapping("dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DateParserService dateParser;

    @GetMapping("aggregate")
    public Object aggregate(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "YEARS") Granularity granularity,
            @RequestParam(required = false) List<DashboardService.DimensionalItem> dimension,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> resourceType,
            @RequestParam(required = false) List<String> rightHolder,
            @RequestParam(required = false) List<String> hasErrors,
            @RequestParam(required = false) List<String> hasWarnings) {

        List<Filter> filters = Map.of(
                        DashboardService.DimensionalItem.STATUS, emptyIfNull(status),
                        DashboardService.DimensionalItem.RESOURCE_TYPE, emptyIfNull(resourceType),
                        DashboardService.DimensionalItem.RIGHT_HOLDER, emptyIfNull(rightHolder),
                        DashboardService.DimensionalItem.HAS_ERRORS, emptyIfNull(hasErrors),
                        DashboardService.DimensionalItem.HAS_WARNINGS, emptyIfNull(hasWarnings))
                .entrySet().stream()
                .filter(e -> isNotEmpty(e.getValue()))
                .map(e -> Filter.of(e.getKey(), e.getValue()))
                .toList();

        return dashboardService.getAggregateData(
                dateParser.parseDateParams(date, startDate, endDate, granularity),
                emptyIfNull(dimension),
                filters);
    }

}
