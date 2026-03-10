package it.gov.innovazione.ndc.harvester.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CookiecutterReferenceServiceTest {

    @Test
    void shouldExtractSemanticHooksFromPreCommitConfig() {
        String config = """
                repos:
                  - repo: https://github.com/teamdigitale/dati-semantic-tools
                    rev: v1.0.0
                    hooks:
                      - id: validate-repo-structure
                      - id: validate-filename-format
                      - id: validate-filename-match-uri
                      - id: validate-filename-match-directory
                      - id: validate-directory-versioning-pattern
                      - id: validate-mandatory-files-presence
                      - id: validate-utf8-file-encoding
                      - id: validate-turtle
                      - id: validate-oas-schema
                      - id: validate-openapi-schema
                      - id: validate-directory-versioning
                      - id: validate-csv
                """;

        List<String> hooks = CookiecutterReferenceService.extractSemanticHooks(config);

        assertThat(hooks).hasSize(12);
        assertThat(hooks).contains(
                "validate-repo-structure",
                "validate-filename-format",
                "validate-turtle",
                "validate-csv");
    }

    @Test
    void shouldReturnEmptyListForNullConfig() {
        assertThat(CookiecutterReferenceService.extractSemanticHooks(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForBlankConfig() {
        assertThat(CookiecutterReferenceService.extractSemanticHooks("")).isEmpty();
    }

    @Test
    void shouldIgnoreNonValidateHooks() {
        String config = """
                repos:
                  - repo: https://github.com/pre-commit/pre-commit-hooks
                    hooks:
                      - id: trailing-whitespace
                      - id: end-of-file-fixer
                  - repo: https://github.com/teamdigitale/dati-semantic-tools
                    hooks:
                      - id: validate-turtle
                """;

        List<String> hooks = CookiecutterReferenceService.extractSemanticHooks(config);

        assertThat(hooks).containsExactly("validate-turtle");
    }
}
