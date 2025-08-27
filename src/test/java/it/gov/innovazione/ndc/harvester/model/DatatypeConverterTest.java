package it.gov.innovazione.ndc.harvester.model;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatatypeConverterTest {

    private static TimeZone originalTz;

    private static LocalDate toLocalDate(Calendar cal) {
        return cal.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @BeforeAll
    static void setUpTz() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid")); // CET/CEST
    }

    @AfterAll
    static void restoreTz() {
        TimeZone.setDefault(originalTz);
    }

    @Test
    void parsesPlainIsoDate() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void parsesWithSpaces() {
        Calendar cal = DatatypeConverter.parseDate("  2025-08-27  ");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void parsesUtcZUppercase() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27Z");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void parsesUtcZLowercase() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27z");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void parsesWithPositiveOffsetColon() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27+02:00");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void parsesWithNegativeOffsetColon() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27-05:30");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void parsesSignedYearPositive() {
        Calendar cal = DatatypeConverter.parseDate("+0025-08-27");
        assertEquals(LocalDate.of(25, 8, 27), toLocalDate(cal));
    }

    // tolleranza extra: offset senza i due punti (±HHMM)
    @Test
    void parsesCompactOffsetPositive_noColon() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27+0200");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void parsesCompactOffsetNegative_noColon() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27-0530");
        assertEquals(LocalDate.of(2025, 8, 27), toLocalDate(cal));
    }

    @Test
    void offsetAheadOfSystemCanShiftToPreviousDay() {
        Calendar cal = DatatypeConverter.parseDate("2025-08-27+14:00");
        assertEquals(LocalDate.of(2025, 8, 26), toLocalDate(cal));
    }

    @Test
    void nullReturnsNull() {
        assertNull(DatatypeConverter.parseDate(null));
    }

    @Test
    void emptyIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DatatypeConverter.parseDate(""));
        assertThrows(IllegalArgumentException.class, () -> DatatypeConverter.parseDate("   "));
    }

    @Test
    void invalidFormatsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DatatypeConverter.parseDate("2025/08/27"));
        assertThrows(IllegalArgumentException.class, () -> DatatypeConverter.parseDate("2025-8-27"));
        assertThrows(IllegalArgumentException.class, () -> DatatypeConverter.parseDate("20250827"));
        assertThrows(IllegalArgumentException.class, () -> DatatypeConverter.parseDate("2025-08-27T12:00:00Z"));
    }
}
