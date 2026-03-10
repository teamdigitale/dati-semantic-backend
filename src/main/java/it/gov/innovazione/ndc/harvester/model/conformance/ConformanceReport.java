package it.gov.innovazione.ndc.harvester.model.conformance;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ConformanceReport {
    private final Instant checkedAt;
    @Singular
    private final List<ConformanceCheck> checks;

    @Data
    @Builder
    public static class ConformanceCheck {
        private final String name;
        private final String category;
        private final CheckResult result;
        private final String details;
    }

    public enum CheckResult {
        PASSED, FAILED, WARNING
    }
}
