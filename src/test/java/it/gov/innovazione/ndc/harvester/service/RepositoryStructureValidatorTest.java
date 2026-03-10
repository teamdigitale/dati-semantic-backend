package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport;
import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport.CheckResult;
import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport.ConformanceCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepositoryStructureValidatorTest {

    private CookiecutterReferenceService cookiecutterReferenceService;
    private RepositoryStructureValidator validator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cookiecutterReferenceService = mock(CookiecutterReferenceService.class);
        validator = new RepositoryStructureValidator(cookiecutterReferenceService);
    }

    @Test
    void shouldReturnEmptyWhenDisabled() {
        when(cookiecutterReferenceService.isEnabled()).thenReturn(false);

        Optional<ConformanceReport> result = validator.validate(tempDir);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenReferenceNotAvailable() {
        when(cookiecutterReferenceService.isEnabled()).thenReturn(true);
        when(cookiecutterReferenceService.getReference()).thenReturn(Optional.empty());

        Optional<ConformanceReport> result = validator.validate(tempDir);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldDetectMissingAssetsDirectory() {
        setupReference();

        ConformanceCheck check = validator.checkAssetsDirectory(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.FAILED);
        assertThat(check.getName()).isEqualTo("assets-directory");
    }

    @Test
    void shouldDetectPresentAssetsDirectory() throws IOException {
        Files.createDirectory(tempDir.resolve("assets"));

        ConformanceCheck check = validator.checkAssetsDirectory(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.PASSED);
    }

    @Test
    void shouldDetectModernAssetTypeDirectories() throws IOException {
        Files.createDirectories(tempDir.resolve("assets/ontologies"));
        Files.createDirectories(tempDir.resolve("assets/controlled-vocabularies"));

        List<ConformanceCheck> checks = validator.checkAssetTypeDirectories(tempDir);

        assertThat(checks).hasSize(3);
        assertThat(checks.stream()
                .filter(c -> c.getResult() == CheckResult.PASSED)
                .count()).isEqualTo(2);
        assertThat(checks.stream()
                .filter(c -> c.getResult() == CheckResult.WARNING)
                .count()).isEqualTo(1);
    }

    @Test
    void shouldDetectLegacyStructure() throws IOException {
        Files.createDirectory(tempDir.resolve("Ontologie"));
        Files.createDirectory(tempDir.resolve("VocabolariControllati"));

        ConformanceCheck check = validator.checkLegacyStructure(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.WARNING);
        assertThat(check.getDetails()).contains("Ontologie");
        assertThat(check.getDetails()).contains("VocabolariControllati");
    }

    @Test
    void shouldPassWhenNoLegacyStructure() {
        ConformanceCheck check = validator.checkLegacyStructure(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.PASSED);
    }

    @Test
    void shouldDetectMissingGitHubWorkflows() {
        ConformanceCheck check = validator.checkGitHubWorkflowsDirectory(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.FAILED);
    }

    @Test
    void shouldDetectPresentGitHubWorkflows() throws IOException {
        Files.createDirectories(tempDir.resolve(".github/workflows"));

        ConformanceCheck check = validator.checkGitHubWorkflowsDirectory(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.PASSED);
    }

    @Test
    void shouldDetectMissingValidateYaml() {
        ConformanceCheck check = validator.checkValidateYamlPresence(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.FAILED);
    }

    @Test
    void shouldDetectPresentValidateYaml() throws IOException {
        Files.createDirectories(tempDir.resolve(".github/workflows"));
        Files.writeString(tempDir.resolve(".github/workflows/validate.yaml"), "name: validate");

        ConformanceCheck check = validator.checkValidateYamlPresence(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.PASSED);
    }

    @Test
    void shouldDetectAlignedValidateYaml() throws IOException {
        String content = "name: validate\non: push\njobs: {}";
        Files.createDirectories(tempDir.resolve(".github/workflows"));
        Files.writeString(tempDir.resolve(".github/workflows/validate.yaml"), content);

        CookiecutterReferenceService.CookiecutterReference ref =
                CookiecutterReferenceService.CookiecutterReference.builder()
                        .validateYaml(content)
                        .preCommitConfig("")
                        .semanticHooks(List.of())
                        .build();

        ConformanceCheck check = validator.checkValidateYamlAlignment(tempDir, ref);

        assertThat(check.getResult()).isEqualTo(CheckResult.PASSED);
    }

    @Test
    void shouldDetectMisalignedValidateYaml() throws IOException {
        Files.createDirectories(tempDir.resolve(".github/workflows"));
        Files.writeString(tempDir.resolve(".github/workflows/validate.yaml"), "name: old");

        CookiecutterReferenceService.CookiecutterReference ref =
                CookiecutterReferenceService.CookiecutterReference.builder()
                        .validateYaml("name: new")
                        .preCommitConfig("")
                        .semanticHooks(List.of())
                        .build();

        ConformanceCheck check = validator.checkValidateYamlAlignment(tempDir, ref);

        assertThat(check.getResult()).isEqualTo(CheckResult.WARNING);
    }

    @Test
    void shouldDetectMissingPreCommitConfig() {
        ConformanceCheck check = validator.checkPreCommitConfigPresence(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.FAILED);
    }

    @Test
    void shouldDetectPresentPreCommitConfig() throws IOException {
        Files.writeString(tempDir.resolve(".pre-commit-config.yaml"), "repos: []");

        ConformanceCheck check = validator.checkPreCommitConfigPresence(tempDir);

        assertThat(check.getResult()).isEqualTo(CheckResult.PASSED);
    }

    @Test
    void shouldDetectAllSemanticHooksPresent() throws IOException {
        String preCommitContent = "repos:\n"
                + "  - repo: https://github.com/teamdigitale/dati-semantic-tools\n"
                + "    hooks:\n"
                + "      - id: validate-repo-structure\n"
                + "      - id: validate-filename-format\n";
        Files.writeString(tempDir.resolve(".pre-commit-config.yaml"), preCommitContent);

        CookiecutterReferenceService.CookiecutterReference ref =
                CookiecutterReferenceService.CookiecutterReference.builder()
                        .validateYaml("")
                        .preCommitConfig("")
                        .semanticHooks(List.of("validate-repo-structure", "validate-filename-format"))
                        .build();

        List<ConformanceCheck> checks = validator.checkSemanticHooks(tempDir, ref);

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getResult()).isEqualTo(CheckResult.PASSED);
        assertThat(checks.get(0).getDetails()).contains("2/2");
    }

    @Test
    void shouldDetectMissingSemanticHooks() throws IOException {
        String preCommitContent = "repos:\n"
                + "  - repo: https://github.com/teamdigitale/dati-semantic-tools\n"
                + "    hooks:\n"
                + "      - id: validate-repo-structure\n";
        Files.writeString(tempDir.resolve(".pre-commit-config.yaml"), preCommitContent);

        CookiecutterReferenceService.CookiecutterReference ref =
                CookiecutterReferenceService.CookiecutterReference.builder()
                        .validateYaml("")
                        .preCommitConfig("")
                        .semanticHooks(List.of("validate-repo-structure", "validate-filename-format",
                                "validate-turtle"))
                        .build();

        List<ConformanceCheck> checks = validator.checkSemanticHooks(tempDir, ref);

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getResult()).isEqualTo(CheckResult.WARNING);
        assertThat(checks.get(0).getDetails()).contains("1/3");
        assertThat(checks.get(0).getDetails()).contains("validate-filename-format");
        assertThat(checks.get(0).getDetails()).contains("validate-turtle");
    }

    @Test
    void shouldProduceFullReport() throws IOException {
        setupReference();

        Files.createDirectories(tempDir.resolve("assets/ontologies"));
        Files.createDirectories(tempDir.resolve(".github/workflows"));
        Files.writeString(tempDir.resolve(".github/workflows/validate.yaml"), "name: validate");
        Files.writeString(tempDir.resolve(".pre-commit-config.yaml"),
                "repos:\n  - hooks:\n      - id: validate-repo-structure\n");

        Optional<ConformanceReport> result = validator.validate(tempDir);

        assertThat(result).isPresent();
        ConformanceReport report = result.get();
        assertThat(report.getCheckedAt()).isNotNull();
        assertThat(report.getChecks()).isNotEmpty();
        assertThat(report.getChecks().stream()
                .map(ConformanceCheck::getName)
                .toList()).contains(
                "assets-directory",
                "no-legacy-structure",
                "github-workflows-directory",
                "validate-yaml-presence",
                "pre-commit-config-presence",
                "semantic-hooks-coverage");
    }

    private void setupReference() {
        when(cookiecutterReferenceService.isEnabled()).thenReturn(true);
        when(cookiecutterReferenceService.getReference()).thenReturn(Optional.of(
                CookiecutterReferenceService.CookiecutterReference.builder()
                        .validateYaml("name: validate")
                        .preCommitConfig("repos: []")
                        .semanticHooks(List.of("validate-repo-structure", "validate-filename-format"))
                        .build()));
    }
}
