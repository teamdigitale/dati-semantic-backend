package it.gov.innovazione.ndc.harvester.model.validation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationReportTest {

    @Test
    void shouldComputeSummaryWithMixedIssues() {
        ValidationReport report = ValidationReport.builder()
                .generatedAt(Instant.now())
                .repositoryUrl("https://github.com/test/repo")
                .repositoryCheck(issue(ValidationIssueSeverity.BLOCKING, "repo check"))
                .repositoryCheck(issue(ValidationIssueSeverity.WARNING, "repo warning"))
                .assetCheck(AssetValidationReport.builder()
                        .assetPath("ont/test.ttl")
                        .assetType("ONTOLOGY")
                        .issue(issue(ValidationIssueSeverity.BLOCKING, "syntax error"))
                        .issue(issue(ValidationIssueSeverity.WARNING, "warning"))
                        .build())
                .assetCheck(AssetValidationReport.builder()
                        .assetPath("cv/test.ttl")
                        .assetType("CONTROLLED_VOCABULARY")
                        .build())
                .build();

        ValidationReport.Summary summary = report.getSummary();

        assertThat(summary.getBlocking()).isEqualTo(2);
        assertThat(summary.getWarning()).isEqualTo(2);
        assertThat(summary.getImprovement()).isZero();
        assertThat(summary.getTotalAssets()).isEqualTo(2);
        assertThat(summary.getAssetsWithIssues()).isEqualTo(1);
    }

    @Test
    void shouldComputeEmptySummary() {
        ValidationReport report = ValidationReport.builder()
                .generatedAt(Instant.now())
                .repositoryUrl("url")
                .build();

        ValidationReport.Summary summary = report.getSummary();

        assertThat(summary.getBlocking()).isZero();
        assertThat(summary.getWarning()).isZero();
        assertThat(summary.getImprovement()).isZero();
        assertThat(summary.getTotalAssets()).isZero();
        assertThat(summary.getAssetsWithIssues()).isZero();
    }

    @Test
    void shouldCountImprovementIssues() {
        ValidationReport report = ValidationReport.builder()
                .generatedAt(Instant.now())
                .repositoryUrl("url")
                .repositoryCheck(issue(ValidationIssueSeverity.IMPROVEMENT, "good"))
                .build();

        assertThat(report.getSummary().getImprovement()).isEqualTo(1);
    }

    @Test
    void shouldIgnoreRepositoryChecksWithoutSeverityInSummary() {
        ValidationReport report = ValidationReport.builder()
                .generatedAt(Instant.now())
                .repositoryUrl("url")
                .repositoryCheck(ValidationIssue.builder()
                        .code("conformance.readme")
                        .name("readme")
                        .category("docs")
                        .result("PASSED")
                        .message("readme [docs]: PASSED")
                        .build())
                .build();

        ValidationReport.Summary summary = report.getSummary();

        assertThat(summary.getBlocking()).isZero();
        assertThat(summary.getWarning()).isZero();
        assertThat(summary.getImprovement()).isZero();
    }

    private static ValidationIssue issue(ValidationIssueSeverity severity, String message) {
        return ValidationIssue.builder()
                .code("test")
                .severity(severity)
                .message(message)
                .build();
    }
}
