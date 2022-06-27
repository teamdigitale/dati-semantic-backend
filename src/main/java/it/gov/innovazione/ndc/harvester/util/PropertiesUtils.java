package it.gov.innovazione.ndc.harvester.util;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PropertiesUtils {

    public static List<String> lowerSkipWords(List<String> skipWords, int minLength) {
        if (skipWords == null) {
            return Collections.emptyList();
        }
        return skipWords.stream()
                .peek(s -> checkMinSkipWordLength(s, minLength))
                .map(sw -> sw.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    private static void checkMinSkipWordLength(String sw, int minLength) {
        if (sw.length() < minLength) {
            throw new IllegalArgumentException(String.format("skip words must be at least %d characters long", minLength));
        }
    }
}
