package it.gov.innovazione.ndc.harvester.validation;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class RdfSyntaxValidationResult {

    @Singular
    private final List<Issue> errors;
    @Singular
    private final List<Issue> warnings;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean isValid() {
        return !hasErrors();
    }

    public String getErrorsSummary() {
        return errors.stream()
                .map(Issue::toString)
                .collect(Collectors.joining("; "));
    }

    public String getWarningsSummary() {
        return warnings.stream()
                .map(Issue::toString)
                .collect(Collectors.joining("; "));
    }

    @Data
    @Builder
    public static class Issue {
        private final long line;
        private final long col;
        private final String message;

        @Override
        public String toString() {
            if (line > 0 && col > 0) {
                return String.format("[line %d, col %d] %s", line, col, message);
            }
            return message;
        }
    }
}
