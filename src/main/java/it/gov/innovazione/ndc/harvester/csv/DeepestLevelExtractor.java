package it.gov.innovazione.ndc.harvester.csv;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Looks for all the columns which represents IDs for different levels, and selects the deepest level as the ID for
 * the whole CSV file.
 *
 * @implNote Currently supports lexicographical order, so it can only effectively handle up to 9 levels...
 */
@Component
@ConditionalOnProperty(value = "harvester.controlled-vocabulary.csv.id-column-extractors.codice-livello.enabled", matchIfMissing = true)
@Order(10)
public class DeepestLevelExtractor implements HeadersToIdNameExtractor {
    private static final Pattern CODICE_LIVELLO_PATTERN = Pattern.compile("codice_(\\d+)_livello", Pattern.CASE_INSENSITIVE);
    private static final Predicate<String> IS_CODICE_LIVELLO_FILTER = CODICE_LIVELLO_PATTERN.asMatchPredicate();

    @Override
    public String extract(List<String> headerNames) {
        return headerNames.stream()
                .filter(IS_CODICE_LIVELLO_FILTER)
                .max(String.CASE_INSENSITIVE_ORDER)
                .orElse(null);
    }
}
