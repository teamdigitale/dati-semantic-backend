package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@RestController
@RequestMapping("dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final Map<String, DashboardService.DimensionalItem> PARAM_MAPPER = Map.of(
            "status", DashboardService.DimensionalItem.STATUS,
            "resourceType", DashboardService.DimensionalItem.RESOURCE_TYPE,
            "rightHolder", DashboardService.DimensionalItem.RIGHT_HOLDER,
            "hasErrors", DashboardService.DimensionalItem.HAS_ERRORS,
            "hasWarnings", DashboardService.DimensionalItem.HAS_WARNINGS);

    private final DashboardService dashboardService;


    @GetMapping("aggregate")
    public Object aggregate(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "YEARS") DashboardService.Granularity granularity,
            @RequestParam(required = false) List<DashboardService.DimensionalItem> dimension,
            @RequestParam MultiValueMap<String, String> params) {

        validateParams(date, startDate, endDate);

        List<DashboardService.Filter> filters = PARAM_MAPPER.keySet()
                .stream()
                .filter(params::containsKey)
                .filter(s -> Objects.nonNull(params.get(s)))
                .filter(s -> !CollectionUtils.isEmpty(params.get(s)))
                .map(s -> DashboardService.Filter.of(
                        PARAM_MAPPER.get(s),
                        params.get(s).stream()
                                .map(ss -> ss.split(","))
                                .flatMap(Stream::of)
                                .toList()))
                .toList();

        return dashboardService.getAggregateData(
                getDateParams(date, startDate, endDate, granularity),
                emptyIfNull(dimension),
                filters);
    }

    private DashboardService.DateParams getDateParams(String date, LocalDate startDate, LocalDate endDate, DashboardService.Granularity granularity) {
        if (date != null) {
            LocalDate now = LocalDate.now();
            return DashboardService.DateParams.of(
                    now,
                    now,
                    granularity);
        }
        return DashboardService.DateParams.of(startDate, endDate, granularity);
    }

    private void validateParams(String date, LocalDate startDate, LocalDate endDate) {
        if (date != null && (startDate != null || endDate != null)) {
            throw new IllegalArgumentException("Only one of date or startDate/endDate must be provided");
        }
    }


}
