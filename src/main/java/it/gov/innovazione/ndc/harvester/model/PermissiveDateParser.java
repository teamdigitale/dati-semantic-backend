package it.gov.innovazione.ndc.harvester.model;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
public final class PermissiveDateParser {

    private static final String[] PATTERNS = {

            // --- Solo data ---
            "yyyy-MM-dd",
            "yyyyMMdd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",

            // --- Data + orario, senza offset ---
            "yyyy-MM-dd'T'HH",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.S",
            "yyyy-MM-dd'T'HH:mm:ss.SS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",

            // Separatore spazio invece di 'T'
            "yyyy-MM-dd HH",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.S",
            "yyyy-MM-dd HH:mm:ss.SS",
            "yyyy-MM-dd HH:mm:ss.SSS",

            // --- Con offset ISO 8601 (X, XX, XXX) ---
            // 'X' accetta anche 'Z' per UTC
            "yyyy-MM-dd'T'HHX",
            "yyyy-MM-dd'T'HH:mmX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SX",
            "yyyy-MM-dd'T'HH:mm:ss.SSX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",

            "yyyy-MM-dd'T'HHXX",
            "yyyy-MM-dd'T'HH:mmXX",
            "yyyy-MM-dd'T'HH:mm:ssXX",
            "yyyy-MM-dd'T'HH:mm:ss.SXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXX",

            "yyyy-MM-dd'T'HHXXX",
            "yyyy-MM-dd'T'HH:mmXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

            // Con spazio come separatore + offset
            "yyyy-MM-dd HHX",
            "yyyy-MM-dd HH:mmX",
            "yyyy-MM-dd HH:mm:ssX",
            "yyyy-MM-dd HH:mm:ss.SX",
            "yyyy-MM-dd HH:mm:ss.SSSX",

            "yyyy-MM-dd HHXX",
            "yyyy-MM-dd HH:mmXX",
            "yyyy-MM-dd HH:mm:ssXX",
            "yyyy-MM-dd HH:mm:ss.SSSXX",

            "yyyy-MM-dd HHXXX",
            "yyyy-MM-dd HH:mmXXX",
            "yyyy-MM-dd HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss.SSSXXX",

            // --- RFC 822 / offset senza i due punti (Z = +0000, +0100, etc.) ---
            "yyyy-MM-dd'T'HHZ",
            "yyyy-MM-dd'T'HH:mmZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd HHZ",
            "yyyy-MM-dd HH:mmZ",
            "yyyy-MM-dd HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss.SSSZ",

            // --- 'Z' letterale suffisso (UTC esplicito) ---
            "yyyy-MM-dd'T'HH'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH'Z'",
            "yyyy-MM-dd HH:mm'Z'",
            "yyyy-MM-dd HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss.SSS'Z'",

            // --- Time zone testuali (meno usate, ma capitano) ---
            "yyyy-MM-dd'T'HH:mm:ss z",
            "yyyy-MM-dd HH:mm:ss z"
    };

    private static final Pattern ISO_DATE_PREFIX = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}");

    /**
     * Parser permissivo: prova molti formati; ritorna sempre una LocalDate.
     * - Se c'è solo la data → usa quella.
     * - Se c'è orario/offset → converte l'istante in UTC e prende la data (LocalDate).
     */
    @SneakyThrows
    public static LocalDate parseToLocalDate(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String s = input.trim();

        // Fast-path: se inizia con yyyy-MM-dd, prova a estrarre subito la data
        if (s.length() >= 10 && ISO_DATE_PREFIX.matcher(s).find()) {
            try {
                return LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignore) {
                log.trace("Fast-path parse failed, falling back to full parser for '{}'", s);
            }
        }

        // DateUtils prova in sequenza i pattern finché uno matcha
        Date d = DateUtils.parseDate(s, Locale.ROOT, PATTERNS);

        // Se c'era un offset/istante, converti in UTC e prendi la data
        return d.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
