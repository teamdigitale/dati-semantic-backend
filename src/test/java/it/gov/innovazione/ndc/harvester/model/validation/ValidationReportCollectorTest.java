package it.gov.innovazione.ndc.harvester.model.validation;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidationResult;
import it.gov.innovazione.ndc.validator.model.ValidationOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationReportCollectorTest {

    @Test
    void shouldAddRepositoryChecks() {
        ValidationReportCollector collector = new ValidationReportCollector();

        ConformanceReport report = ConformanceReport.builder()
                .checkedAt(Instant.now())
                .check(ConformanceReport.ConformanceCheck.builder()
                        .name("assets-directory")
                        .category("structure")
                        .result(ConformanceReport.CheckResult.FAILED)
                        .details("Missing assets directory")
                        .build())
                .check(ConformanceReport.ConformanceCheck.builder()
                        .name("readme")
                        .category("docs")
                        .result(ConformanceReport.CheckResult.WARNING)
                        .build())
                .check(ConformanceReport.ConformanceCheck.builder()
                        .name("license")
                        .category("docs")
                        .result(ConformanceReport.CheckResult.PASSED)
                        .build())
                .build();

        collector.addRepositoryChecks(report);

        ValidationReport result = collector.build("https://github.com/test/repo", "main");

        assertThat(result.getRepositoryChecks()).hasSize(3);
        assertThat(result.getRepositoryChecks().get(0).getSeverity()).isEqualTo(ValidationIssueSeverity.BLOCKING);
        assertThat(result.getRepositoryChecks().get(0).getCode()).isEqualTo("conformance.assets-directory");
        assertThat(result.getRepositoryChecks().get(0).getResult()).isEqualTo("FAILED");
        assertThat(result.getRepositoryChecks().get(1).getSeverity()).isEqualTo(ValidationIssueSeverity.WARNING);
        assertThat(result.getRepositoryChecks().get(2).getSeverity()).isNull();
        assertThat(result.getRepositoryChecks().get(2).getResult()).isEqualTo("PASSED");
    }

    @Test
    void shouldHandleNullConformanceReport() {
        ValidationReportCollector collector = new ValidationReportCollector();
        collector.addRepositoryChecks(null);

        ValidationReport result = collector.build("url", null);
        assertThat(result.getRepositoryChecks()).isEmpty();
    }

    @Test
    void shouldAddSyntaxErrors() {
        ValidationReportCollector collector = new ValidationReportCollector();

        RdfSyntaxValidationResult syntaxResult = RdfSyntaxValidationResult.builder()
                .error(RdfSyntaxValidationResult.Issue.builder()
                        .line(10).col(5).message("Bad syntax").build())
                .warning(RdfSyntaxValidationResult.Issue.builder()
                        .line(20).col(0).message("Deprecated prefix").build())
                .build();

        collector.addSyntaxResult("ontologies/test.ttl", SemanticAssetType.ONTOLOGY, syntaxResult);

        ValidationReport result = collector.build("url", null);

        assertThat(result.getAssetChecks()).hasSize(1);
        AssetValidationReport asset = result.getAssetChecks().get(0);
        assertThat(asset.getAssetPath()).isEqualTo("ontologies/test.ttl");
        assertThat(asset.getAssetType()).isEqualTo("ONTOLOGY");
        assertThat(asset.getIssues()).hasSize(2);
        assertThat(asset.getIssues().get(0).getSeverity()).isEqualTo(ValidationIssueSeverity.BLOCKING);
        assertThat(asset.getIssues().get(0).getLine()).isEqualTo(10L);
        assertThat(asset.getIssues().get(0).getCol()).isEqualTo(5L);
        assertThat(asset.getIssues().get(1).getSeverity()).isEqualTo(ValidationIssueSeverity.WARNING);
        assertThat(asset.getIssues().get(1).getCol()).isNull();
    }

    @Test
    void shouldAddMetadataResults() {
        ValidationReportCollector collector = new ValidationReportCollector();

        RdfSyntaxValidationResult syntaxOk = RdfSyntaxValidationResult.builder().build();
        collector.addSyntaxResult("schemas/test.ttl", SemanticAssetType.SCHEMA, syntaxOk);

        SemanticAssetModelValidationContext ctx =
                SemanticAssetModelValidationContext.getForValidation();
        ctx.getErrors().add(new ValidationOutcome(
                "dcterms:title",
                "Title is missing",
                new RuntimeException("Title is missing")));
        ctx.getWarnings().add(new ValidationOutcome(
                "dcterms:description",
                "Description too short",
                new RuntimeException("Description too short")));

        collector.addMetadataResult("schemas/test.ttl", ctx);

        ValidationReport result = collector.build("url", null);

        AssetValidationReport asset = result.getAssetChecks().get(0);
        assertThat(asset.getIssues()).hasSize(2);
        assertThat(asset.getIssues().get(0).getSeverity()).isEqualTo(ValidationIssueSeverity.BLOCKING);
        assertThat(asset.getIssues().get(0).getField()).isEqualTo("dcterms:title");
        assertThat(asset.getIssues().get(1).getSeverity()).isEqualTo(ValidationIssueSeverity.WARNING);
    }

    @Test
    void shouldIgnoreMetadataForUnknownAsset() {
        ValidationReportCollector collector = new ValidationReportCollector();

        SemanticAssetModelValidationContext ctx =
                SemanticAssetModelValidationContext.getForValidation();
        ctx.getErrors().add(new ValidationOutcome(
                "field",
                "msg",
                new RuntimeException("msg")));

        collector.addMetadataResult("unknown/path.ttl", ctx);

        ValidationReport result = collector.build("url", null);
        assertThat(result.getAssetChecks()).isEmpty();
    }

    @Test
    void shouldHandleNullMetadataContext() {
        ValidationReportCollector collector = new ValidationReportCollector();
        collector.addMetadataResult("path.ttl", null);

        ValidationReport result = collector.build("url", null);
        assertThat(result.getAssetChecks()).isEmpty();
    }

    @Test
    void shouldMarkAssetSkipped() {
        ValidationReportCollector collector = new ValidationReportCollector();

        RdfSyntaxValidationResult result = RdfSyntaxValidationResult.builder()
                .error(RdfSyntaxValidationResult.Issue.builder()
                        .message("error").build())
                .build();
        collector.addSyntaxResult("test.ttl", SemanticAssetType.ONTOLOGY, result);
        collector.markAssetSkipped("test.ttl");

        ValidationReport report = collector.build("url", null);
        assertThat(report.getAssetChecks().get(0).isSkipped()).isTrue();
    }

    @Test
    void shouldTrackTotalAssets() {
        ValidationReportCollector collector = new ValidationReportCollector();
        assertThat(collector.getTotalAssets()).isEqualTo(-1);

        collector.setTotalAssets(5);
        assertThat(collector.getTotalAssets()).isEqualTo(5);
    }

    @Test
    void shouldTrackProcessedAssets() {
        ValidationReportCollector collector = new ValidationReportCollector();
        assertThat(collector.getProcessedAssets().get()).isZero();

        collector.getProcessedAssets().incrementAndGet();
        assertThat(collector.getProcessedAssets().get()).isEqualTo(1);
    }

    @Test
    void shouldBuildWithRepoUrlAndRevision() {
        ValidationReportCollector collector = new ValidationReportCollector();

        ValidationReport result = collector.build("https://github.com/owner/repo", "abc123");

        assertThat(result.getRepositoryUrl()).isEqualTo("https://github.com/owner/repo");
        assertThat(result.getRevision()).isEqualTo("abc123");
        assertThat(result.getGeneratedAt()).isNotNull();
    }
}
