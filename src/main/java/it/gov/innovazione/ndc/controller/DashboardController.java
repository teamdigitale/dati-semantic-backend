package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.controller.date.DateParameter.Granularity;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import it.gov.innovazione.ndc.service.DashboardService;
import it.gov.innovazione.ndc.service.DimensionalItem;
import it.gov.innovazione.ndc.service.DimensionalItem.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static it.gov.innovazione.ndc.service.DimensionalItem.Filter.getSemanticContentFilters;
import static it.gov.innovazione.ndc.service.DimensionalItem.Filter.getTimeDataFilters;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@RestController
@RequestMapping("dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DateParserService dateParser;

    @GetMapping("aggregated-count-data")
    public AggregateDashboardResponse aggregatedCount(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "YEARS") Granularity granularity,
            @RequestParam(required = false) List<DimensionalItem.CountDataDimensionalItem> dimension,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> resourceType,
            @RequestParam(required = false) List<String> rightHolder,
            @RequestParam(required = false) List<String> hasErrors,
            @RequestParam(required = false) List<String> hasWarnings) {

        List<Filter<SemanticContentStats>> filters = getSemanticContentFilters(status, resourceType, rightHolder, hasErrors, hasWarnings);

        return dashboardService.getAggregateCountData(
                dateParser.parseDateParams(date, startDate, endDate, granularity),
                emptyIfNull(dimension),
                filters);
    }

    @GetMapping("aggregated-time-data")
    public AggregateDashboardResponse aggregatedTime(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "YEARS") Granularity granularity,
            @RequestParam(required = false) List<DimensionalItem.TimeDataDimensionalItem> dimension,
            @RequestParam(required = false) List<String> repositoryUrl) {

        List<Filter<HarvesterRun>> filters = getTimeDataFilters(repositoryUrl);

        return dashboardService.getAggregateTimeData(
                dateParser.parseDateParams(date, startDate, endDate, granularity),
                emptyIfNull(dimension),
                filters);
    }

    @GetMapping(value = "raw-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedSemanticContentStats rawJson(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> resourceType,
            @RequestParam(required = false) List<String> rightHolder,
            @RequestParam(required = false) List<String> hasErrors,
            @RequestParam(required = false) List<String> hasWarnings,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return getPagedSemanticContentStats(startDate, endDate, status, resourceType, rightHolder, hasErrors, hasWarnings, page, size);
    }

    @GetMapping(value = "raw-data", produces = "text/csv")
    public String rawCsv(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> resourceType,
            @RequestParam(required = false) List<String> rightHolder,
            @RequestParam(required = false) List<String> hasErrors,
            @RequestParam(required = false) List<String> hasWarnings,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedSemanticContentStats pagedSemanticContentStats = getPagedSemanticContentStats(startDate, endDate, status, resourceType, rightHolder, hasErrors, hasWarnings, page, size);
        return CsvUtils.writeCsv(Stream.concat(
                        Stream.of(pagedSemanticContentStats.getHeaders()),
                        pagedSemanticContentStats.getContent().stream())
                .toList());
    }

    private PagedSemanticContentStats getPagedSemanticContentStats(
            LocalDate startDate,
            LocalDate endDate,
            List<String> status,
            List<String> resourceType,
            List<String> rightHolder,
            List<String> hasErrors,
            List<String> hasWarnings,
            int page,
            int size) {
        List<Filter<SemanticContentStats>> filters = getSemanticContentFilters(status, resourceType, rightHolder, hasErrors, hasWarnings);
        return PagedSemanticContentStats.of(dashboardService.getRawData(startDate, endDate, filters), page, size);
    }


    private static class CsvUtils {

        public static String writeCsv(List<List<String>> csvContent) {
            StringBuilder builder = new StringBuilder();
            csvContent.forEach(
                    row -> {
                        builder.append(String.join(",", row))
                                .append("\n");
                    });
            return builder.toString();
        }
    }
}
