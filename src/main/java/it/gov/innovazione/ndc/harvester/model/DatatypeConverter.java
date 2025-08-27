package it.gov.innovazione.ndc.harvester.model;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DatatypeConverter {

    private static final DateTimeFormatter XSD_DATE_STD = new DateTimeFormatterBuilder()
            .parseCaseSensitive()
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .optionalStart()
            .appendOffset("+HH:MM", "Z")
            .optionalEnd()
            .toFormatter(Locale.ROOT);

    private static final Pattern COMPACT_OFFSET = Pattern.compile(
            "^\\s*([+-]?\\d{4,})-(\\d{2})-(\\d{2})([Zz]|[+-]\\d{2}:?\\d{2})?\\s*$"
    );

    public static Calendar parseDate(String date) {
        if (date == null) {
            return null;
        }
        String s = date.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty xs:date");
        }

        try {
            var parsed = XSD_DATE_STD.parse(s);
            LocalDate localDate = LocalDate.of(
                    parsed.get(ChronoField.YEAR),
                    parsed.get(ChronoField.MONTH_OF_YEAR),
                    parsed.get(ChronoField.DAY_OF_MONTH)
            );
            ZoneOffset offset = parsed.isSupported(ChronoField.OFFSET_SECONDS)
                    ? ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS))
                    : ZoneOffset.UTC;

            Instant instant = OffsetDateTime.of(localDate, LocalTime.MIDNIGHT, offset).toInstant();

            return GregorianCalendar.from(instant.atZone(
                    parsed.isSupported(ChronoField.OFFSET_SECONDS)
                            ? offset
                            : ZoneId.systemDefault()));

        } catch (Exception ignore) {
            log.warn("Failed to parse xs:date '{}' with standard parser, falling back to regex", s);
        }

        Matcher m = COMPACT_OFFSET.matcher(s);
        if (m.matches()) {
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day = Integer.parseInt(m.group(3));
            String off = m.group(4);

            LocalDate datePart = LocalDate.of(year, month, day);
            ZoneId zone = ZoneId.systemDefault();
            if (off != null) {
                if (off.equalsIgnoreCase("Z")) {
                    zone = ZoneOffset.UTC;
                } else {
                    String norm = off.contains(":") ? off : (off.substring(0, 3) + ":" + off.substring(3));
                    zone = ZoneOffset.of(norm);
                }
            }
            Instant instant = datePart.atStartOfDay(zone).toInstant();
            return GregorianCalendar.from(instant.atZone(zone));
        }

        throw new IllegalArgumentException("Invalid xs:date: " + date);
    }
}
