package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport;
import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport.CheckResult;
import it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport.ConformanceCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport.CheckResult.FAILED;
import static it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport.CheckResult.PASSED;
import static it.gov.innovazione.ndc.harvester.model.conformance.ConformanceReport.CheckResult.WARNING;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryStructureValidator {

    private static final String CAT_STRUCTURE = "structure";
    private static final String CAT_CI = "ci";
    private static final String CAT_HOOKS = "hooks";

    private final CookiecutterReferenceService cookiecutterReferenceService;

    public Optional<ConformanceReport> validate(Path repoPath) {
        if (!cookiecutterReferenceService.isEnabled()) {
            log.info("Conformance validation is disabled");
            return Optional.empty();
        }

        Optional<CookiecutterReferenceService.CookiecutterReference> refOpt =
                cookiecutterReferenceService.getReference();

        if (refOpt.isEmpty()) {
            log.warn("Could not load cookiecutter reference, skipping conformance check");
            return Optional.empty();
        }

        CookiecutterReferenceService.CookiecutterReference reference = refOpt.get();
        List<ConformanceCheck> checks = new ArrayList<>();

        checks.add(checkAssetsDirectory(repoPath));
        checks.addAll(checkAssetTypeDirectories(repoPath));
        checks.add(checkLegacyStructure(repoPath));
        checks.add(checkGitHubWorkflowsDirectory(repoPath));
        checks.add(checkValidateYamlPresence(repoPath));
        checks.add(checkValidateYamlAlignment(repoPath, reference));
        checks.add(checkPreCommitConfigPresence(repoPath));
        checks.addAll(checkSemanticHooks(repoPath, reference));

        return Optional.of(ConformanceReport.builder()
                .checkedAt(Instant.now())
                .checks(checks)
                .build());
    }

    ConformanceCheck checkAssetsDirectory(Path repoPath) {
        boolean exists = Files.isDirectory(repoPath.resolve("assets"));
        return ConformanceCheck.builder()
                .name("assets-directory")
                .category(CAT_STRUCTURE)
                .result(exists ? PASSED : FAILED)
                .details(exists
                        ? "assets/ directory found"
                        : "assets/ directory missing - expected by cookiecutter template")
                .build();
    }

    List<ConformanceCheck> checkAssetTypeDirectories(Path repoPath) {
        List<ConformanceCheck> checks = new ArrayList<>();
        for (SemanticAssetType type : SemanticAssetType.values()) {
            boolean modern = Files.isDirectory(repoPath.resolve(type.getFolderName()));
            checks.add(ConformanceCheck.builder()
                    .name("asset-type-" + type.name().toLowerCase().replace('_', '-'))
                    .category(CAT_STRUCTURE)
                    .result(modern ? PASSED : WARNING)
                    .details(modern
                            ? type.getFolderName() + "/ directory found"
                            : type.getFolderName() + "/ directory missing")
                    .build());
        }
        return checks;
    }

    ConformanceCheck checkLegacyStructure(Path repoPath) {
        List<String> legacyDirs = new ArrayList<>();
        for (SemanticAssetType type : SemanticAssetType.values()) {
            if (Files.isDirectory(repoPath.resolve(type.getLegacyFolderName()))) {
                legacyDirs.add(type.getLegacyFolderName());
            }
        }
        if (legacyDirs.isEmpty()) {
            return ConformanceCheck.builder()
                    .name("no-legacy-structure")
                    .category(CAT_STRUCTURE)
                    .result(PASSED)
                    .details("No legacy directory structure detected")
                    .build();
        }
        return ConformanceCheck.builder()
                .name("no-legacy-structure")
                .category(CAT_STRUCTURE)
                .result(WARNING)
                .details("Legacy directories found: " + String.join(", ", legacyDirs)
                        + " - consider migrating to assets/ structure")
                .build();
    }

    ConformanceCheck checkGitHubWorkflowsDirectory(Path repoPath) {
        boolean exists = Files.isDirectory(repoPath.resolve(".github/workflows"));
        return ConformanceCheck.builder()
                .name("github-workflows-directory")
                .category(CAT_CI)
                .result(exists ? PASSED : FAILED)
                .details(exists
                        ? ".github/workflows/ directory found"
                        : ".github/workflows/ directory missing")
                .build();
    }

    ConformanceCheck checkValidateYamlPresence(Path repoPath) {
        boolean exists = Files.isRegularFile(repoPath.resolve(".github/workflows/validate.yaml"));
        return ConformanceCheck.builder()
                .name("validate-yaml-presence")
                .category(CAT_CI)
                .result(exists ? PASSED : FAILED)
                .details(exists
                        ? ".github/workflows/validate.yaml found"
                        : ".github/workflows/validate.yaml missing")
                .build();
    }

    ConformanceCheck checkValidateYamlAlignment(Path repoPath,
                                                CookiecutterReferenceService.CookiecutterReference reference) {
        Path validatePath = repoPath.resolve(".github/workflows/validate.yaml");
        if (!Files.isRegularFile(validatePath)) {
            return ConformanceCheck.builder()
                    .name("validate-yaml-alignment")
                    .category(CAT_CI)
                    .result(FAILED)
                    .details("Cannot check alignment: validate.yaml not found in repository")
                    .build();
        }
        if (reference.getValidateYaml() == null) {
            return ConformanceCheck.builder()
                    .name("validate-yaml-alignment")
                    .category(CAT_CI)
                    .result(WARNING)
                    .details("Cannot check alignment: reference validate.yaml not available")
                    .build();
        }

        try {
            String repoContent = Files.readString(validatePath, StandardCharsets.UTF_8);
            boolean aligned = repoContent.trim().equals(reference.getValidateYaml().trim());
            return ConformanceCheck.builder()
                    .name("validate-yaml-alignment")
                    .category(CAT_CI)
                    .result(aligned ? PASSED : WARNING)
                    .details(aligned
                            ? "validate.yaml is aligned with cookiecutter template"
                            : "validate.yaml differs from cookiecutter template")
                    .build();
        } catch (IOException e) {
            return ConformanceCheck.builder()
                    .name("validate-yaml-alignment")
                    .category(CAT_CI)
                    .result(WARNING)
                    .details("Error reading validate.yaml: " + e.getMessage())
                    .build();
        }
    }

    ConformanceCheck checkPreCommitConfigPresence(Path repoPath) {
        boolean exists = Files.isRegularFile(repoPath.resolve(".pre-commit-config.yaml"));
        return ConformanceCheck.builder()
                .name("pre-commit-config-presence")
                .category(CAT_HOOKS)
                .result(exists ? PASSED : FAILED)
                .details(exists
                        ? ".pre-commit-config.yaml found"
                        : ".pre-commit-config.yaml missing")
                .build();
    }

    List<ConformanceCheck> checkSemanticHooks(Path repoPath,
                                              CookiecutterReferenceService.CookiecutterReference reference) {
        List<String> expectedHooks = reference.getSemanticHooks();
        if (expectedHooks == null || expectedHooks.isEmpty()) {
            return Collections.singletonList(ConformanceCheck.builder()
                    .name("semantic-hooks")
                    .category(CAT_HOOKS)
                    .result(WARNING)
                    .details("No expected semantic hooks defined in cookiecutter reference")
                    .build());
        }

        Path preCommitPath = repoPath.resolve(".pre-commit-config.yaml");
        if (!Files.isRegularFile(preCommitPath)) {
            return Collections.singletonList(ConformanceCheck.builder()
                    .name("semantic-hooks")
                    .category(CAT_HOOKS)
                    .result(FAILED)
                    .details("Cannot check hooks: .pre-commit-config.yaml not found")
                    .build());
        }

        List<String> repoHooks = extractHookIds(preCommitPath);

        List<ConformanceCheck> checks = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> present = new ArrayList<>();

        for (String hook : expectedHooks) {
            if (repoHooks.contains(hook)) {
                present.add(hook);
            } else {
                missing.add(hook);
            }
        }

        CheckResult overallResult = missing.isEmpty() ? PASSED
                : missing.size() < expectedHooks.size() ? WARNING : FAILED;

        checks.add(ConformanceCheck.builder()
                .name("semantic-hooks-coverage")
                .category(CAT_HOOKS)
                .result(overallResult)
                .details(String.format("%d/%d semantic hooks present. Present: [%s]%s",
                        present.size(), expectedHooks.size(),
                        String.join(", ", present),
                        missing.isEmpty() ? "" : ". Missing: [" + String.join(", ", missing) + "]"))
                .build());

        return checks;
    }

    private List<String> extractHookIds(Path preCommitPath) {
        try {
            String content = Files.readString(preCommitPath, StandardCharsets.UTF_8);
            return CookiecutterReferenceService.extractSemanticHooks(content);
        } catch (IOException e) {
            log.warn("Could not read pre-commit config: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
