package it.gov.innovazione.ndc.harvester.model.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationReport {
    private final Instant generatedAt;
    private final String repositoryUrl;
    private final String revision;
    @Singular("repositoryCheck")
    private final List<ValidationIssue> repositoryChecks;
    @Singular("assetCheck")
    private final List<AssetValidationReport> assetChecks;

    public Summary getSummary() {
        Map<ValidationIssueSeverity, Long> allIssues = Stream.concat(
                        repositoryChecks.stream().map(ValidationIssue::getSeverity),
                        assetChecks.stream()
                                .flatMap(a -> a.getIssues().stream())
                                .map(ValidationIssue::getSeverity))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        long assetsWithIssues = assetChecks.stream()
                .filter(a -> !a.getIssues().isEmpty())
                .count();

        return Summary.builder()
                .blocking(allIssues.getOrDefault(ValidationIssueSeverity.BLOCKING, 0L))
                .warning(allIssues.getOrDefault(ValidationIssueSeverity.WARNING, 0L))
                .improvement(allIssues.getOrDefault(ValidationIssueSeverity.IMPROVEMENT, 0L))
                .totalAssets(assetChecks.size())
                .assetsWithIssues(assetsWithIssues)
                .build();
    }

    @Data
    @Builder
    public static class Summary {
        private final long blocking;
        private final long warning;
        private final long improvement;
        private final long totalAssets;
        private final long assetsWithIssues;
    }
}
