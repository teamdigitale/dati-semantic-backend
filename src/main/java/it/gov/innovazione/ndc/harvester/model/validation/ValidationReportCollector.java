package it.gov.innovazione.ndc.harvester.model.validation;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidationResult;
import it.gov.innovazione.ndc.validator.model.ValidationOutcome;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ValidationReportCollector {

    private final List<ValidationIssue> repositoryChecks = new ArrayList<>();
    private final Map<String, AssetEntry> assetEntries = new LinkedHashMap<>();

    @Getter
    private final AtomicInteger processedAssets = new AtomicInteger(0);
    private volatile int totalAssets = -1;

    public synchronized int getTotalAssets() {
        return totalAssets;
    }

    public synchronized void setTotalAssets(int total) {
        this.totalAssets = total;
    }

    public synchronized void addRepositoryChecks(ConformanceReport report) {
        if (report == null) {
            return;
        }
        for (ConformanceReport.ConformanceCheck check : report.getChecks()) {
            repositoryChecks.add(ValidationIssue.builder()
                    .code("conformance." + check.getName())
                    .severity(mapCheckResult(check.getResult()))
                    .message(formatConformanceMessage(check))
                    .name(check.getName())
                    .category(check.getCategory())
                    .result(check.getResult().name())
                    .details(check.getDetails())
                    .build());
        }
    }

    public synchronized void addSyntaxResult(String relativePath, SemanticAssetType type,
                                              RdfSyntaxValidationResult result) {
        AssetEntry entry = getOrCreateEntry(relativePath, type);
        for (RdfSyntaxValidationResult.Issue error : result.getErrors()) {
            entry.issues.add(ValidationIssue.builder()
                    .code("syntax.error")
                    .severity(ValidationIssueSeverity.BLOCKING)
                    .message(error.getMessage())
                    .line(error.getLine() > 0 ? error.getLine() : null)
                    .col(error.getCol() > 0 ? error.getCol() : null)
                    .build());
        }
        for (RdfSyntaxValidationResult.Issue warning : result.getWarnings()) {
            entry.issues.add(ValidationIssue.builder()
                    .code("syntax.warning")
                    .severity(ValidationIssueSeverity.WARNING)
                    .message(warning.getMessage())
                    .line(warning.getLine() > 0 ? warning.getLine() : null)
                    .col(warning.getCol() > 0 ? warning.getCol() : null)
                    .build());
        }
    }

    public synchronized void addMetadataResult(String relativePath,
                                                SemanticAssetModelValidationContext ctx) {
        if (ctx == null) {
            return;
        }
        AssetEntry entry = assetEntries.get(relativePath);
        if (entry == null) {
            log.warn("Metadata result for unknown asset path '{}', skipping. Known paths: {}",
                    relativePath, assetEntries.keySet());
            return;
        }
        for (ValidationOutcome error : ctx.getErrors()) {
            entry.issues.add(ValidationIssue.builder()
                    .code("metadata.error." + normalizeFieldName(error.getFieldName()))
                    .severity(ValidationIssueSeverity.BLOCKING)
                    .message(error.getMessage())
                    .field(error.getFieldName())
                    .build());
        }
        for (ValidationOutcome warning : ctx.getWarnings()) {
            entry.issues.add(ValidationIssue.builder()
                    .code("metadata.warning." + normalizeFieldName(warning.getFieldName()))
                    .severity(ValidationIssueSeverity.WARNING)
                    .message(warning.getMessage())
                    .field(warning.getFieldName())
                    .build());
        }
    }

    public synchronized void markAssetSkipped(String relativePath) {
        AssetEntry entry = assetEntries.get(relativePath);
        if (entry != null) {
            entry.skipped = true;
        }
    }

    public synchronized ValidationReport build(String repoUrl, String revision) {
        List<AssetValidationReport> assetReports = new ArrayList<>();
        for (Map.Entry<String, AssetEntry> e : assetEntries.entrySet()) {
            AssetEntry entry = e.getValue();
            assetReports.add(AssetValidationReport.builder()
                    .assetPath(e.getKey())
                    .assetType(entry.assetType)
                    .issues(new ArrayList<>(entry.issues))
                    .skipped(entry.skipped)
                    .build());
        }
        return ValidationReport.builder()
                .generatedAt(Instant.now())
                .repositoryUrl(repoUrl)
                .revision(revision)
                .repositoryChecks(new ArrayList<>(repositoryChecks))
                .assetChecks(assetReports)
                .build();
    }

    private AssetEntry getOrCreateEntry(String relativePath, SemanticAssetType type) {
        return assetEntries.computeIfAbsent(relativePath,
                k -> new AssetEntry(type != null ? type.name() : null));
    }

    private static ValidationIssueSeverity mapCheckResult(ConformanceReport.CheckResult result) {
        return switch (result) {
            case FAILED -> ValidationIssueSeverity.BLOCKING;
            case WARNING -> ValidationIssueSeverity.WARNING;
            case PASSED -> null;
        };
    }

    private static String formatConformanceMessage(ConformanceReport.ConformanceCheck check) {
        String base = check.getName() + " [" + check.getCategory() + "]: " + check.getResult();
        if (check.getDetails() != null && !check.getDetails().isEmpty()) {
            return base + " - " + check.getDetails();
        }
        return base;
    }

    private static String normalizeFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "unknown";
        }
        return fieldName.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
    }

    private static class AssetEntry {
        final String assetType;
        final List<ValidationIssue> issues = new ArrayList<>();
        boolean skipped;

        AssetEntry(String assetType) {
            this.assetType = assetType;
        }
    }
}
