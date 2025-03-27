package it.gov.innovazione.ndc.controller.date;

import com.apicatalog.jsonld.StringUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.format;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DateParameter {

    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2023, 6, 1);

    private final LocalDate startDate;
    private final LocalDate endDate;
    @Getter
    private final Granularity granularity;

    public static DateParameter of(String date, String startDate, String endDate, Granularity granularity) {
        if (!StringUtils.isBlank(date)) {
            DateFormat dateFormat = DateFormat.from(date)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid date format for date " + date + ". Supported formats are: " + DateFormat.getSupportedFormats()));
            if (dateFormat.getUnsupportedGranularities().contains(granularity)) {
                throw new IllegalArgumentException("Granularity " + granularity + " is not supported for date of format " + dateFormat.getFormat());
            }
            LocalDate localStartDate = ofDate(date, dateFormat);
            LocalDate localEndDate = localStartDate.with(dateFormat.getEquivalentGranularity().getEndDateAdjuster());
            return new DateParameter(
                    localStartDate,
                    localEndDate,
                    granularity);
        }
        LocalDate localStartDate = null;
        LocalDate localEndDate = null;
        if (!StringUtils.isBlank(startDate)) {
            DateFormat dateFormat = DateFormat.from(startDate)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid date format for startDate " + startDate + ". Supported formats are: " + DateFormat.getSupportedFormats()));
            localStartDate = ofDate(startDate, dateFormat);
        }
        if (!StringUtils.isBlank(endDate)) {
            DateFormat dateFormat = DateFormat.from(endDate)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid date format for endDate " + endDate + ". Supported formats are: " + DateFormat.getSupportedFormats()));
            localEndDate = ofDate(endDate, dateFormat)
                    .with(dateFormat.getEquivalentGranularity().getEndDateAdjuster());

        }
        if (localStartDate == null) {
            localStartDate = DEFAULT_START_DATE.with(granularity.getStartDateAdjuster());
        }
        if (localEndDate == null) {
            localEndDate = LocalDate.now().with(granularity.getEndDateAdjuster());
        }
        return new DateParameter(
                localStartDate,
                localEndDate,
                granularity);
    }

    private static LocalDate ofDate(String date, DateFormat dateFormat) {
        return switch (dateFormat) {
            case YEAR -> LocalDate.of(Integer.parseInt(date), 1, 1);
            case MONTH -> {
                String[] parts = date.split("-");
                Integer year = Integer.parseInt(parts[1]);
                Integer month = Integer.parseInt(parts[0]);
                yield LocalDate.of(year, month, 1);
            }
            case DAY -> {
                String[] parts = date.split("-");
                Integer year = Integer.parseInt(parts[2]);
                Integer month = Integer.parseInt(parts[1]);
                Integer day = Integer.parseInt(parts[0]);
                yield LocalDate.of(year, month, day);
            }
        };
    }

    public List<LocalDate> getDates() {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = startDate; date.isBefore(endDate); date = getDateIncrement().apply(date)) {
            dates.add(date);
        }
        return dates;
    }

    public Function<LocalDate, LocalDate> getDateIncrement() {
        return localDate -> localDate.plus(1, granularity.getUnit());
    }

    public Function<LocalDate, String> getDateFormatter() {
        return granularity.getFormatter();
    }

    @RequiredArgsConstructor
    @Getter
    public enum DateFormat {
        YEAR("YYYY",
                Pattern.compile("^\\d{4}$"),
                List.of(),
                Granularity.YEARS),
        MONTH("MM-YYYY",
                Pattern.compile("^\\d{2}-\\d{4}$"),
                List.of(
                        Granularity.YEARS,
                        Granularity.WEEKS_IN_YEAR),
                Granularity.MONTHS),
        DAY("DD-MM-YYYY",
                Pattern.compile("^\\d{2}-\\d{2}-\\d{4}$"),
                List.of(
                        Granularity.YEARS,
                        Granularity.MONTHS,
                        Granularity.WEEKS_IN_MONTH,
                        Granularity.WEEKS_IN_YEAR),
                Granularity.DAYS);

        private final String format;
        private final Pattern pattern;
        private final List<Granularity> unsupportedGranularities;
        private final Granularity equivalentGranularity;

        public static Optional<DateFormat> from(String date) {
            return Arrays.stream(DateFormat.values())
                    .filter(df -> df.getPattern().matcher(date).matches())
                    .findFirst();
        }

        public static List<String> getSupportedFormats() {
            return Arrays.stream(DateFormat.values())
                    .map(DateFormat::getFormat)
                    .toList();
        }
    }

    @RequiredArgsConstructor
    @Getter
    public enum Granularity {
        YEARS(ChronoUnit.YEARS,
                TemporalAdjusters.firstDayOfYear(),
                date -> String.valueOf(date.getYear())),
        MONTHS(ChronoUnit.MONTHS,
                TemporalAdjusters.firstDayOfMonth(),
                date ->
                        format("%02d-%d",
                                date.getMonthValue(),
                                date.getYear())),
        WEEKS_IN_MONTH(ChronoUnit.WEEKS,
                TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY),
                date ->
                        format("W%d-%02d-%d",
                                date.get(WeekFields.of(DayOfWeek.SUNDAY, 1).weekOfMonth()),
                                date.getMonthValue(),
                                date.getYear())),
        WEEKS_IN_YEAR(ChronoUnit.WEEKS,
                TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY),
                date ->
                        format("W%d-%d",
                                date.get(WeekFields.of(DayOfWeek.SUNDAY, 1).weekOfYear()),
                                date.getYear())),
        DAYS(ChronoUnit.DAYS, a -> a,
                date ->
                        format("%02d-%02d-%d",
                                date.getDayOfMonth(),
                                date.getMonthValue(),
                                date.getYear()));


        private final TemporalUnit unit;
        private final TemporalAdjuster startDateAdjuster;
        private final Function<LocalDate, String> formatter;

        private TemporalAdjuster getEndDateAdjuster() {
            return date ->
                    ((LocalDate) date)
                            .with(startDateAdjuster)
                            .plus(1, unit);
        }
    }


}
