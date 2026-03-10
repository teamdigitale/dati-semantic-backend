package it.gov.innovazione.ndc.harvester.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RdfSyntaxValidatorTest {

    private RdfSyntaxValidator validator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validator = new RdfSyntaxValidator();
    }

    @Test
    void shouldPassForValidTurtle() throws IOException {
        Path ttlFile = tempDir.resolve("valid.ttl");
        Files.writeString(ttlFile, """
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix dct:  <http://purl.org/dc/terms/> .

                <http://example.org/vocab/1>
                    a skos:ConceptScheme ;
                    dct:title "Test Vocabulary"@it .
                """);

        RdfSyntaxValidationResult result = validator.validateTurtle(ttlFile.toString());

        assertThat(result.isValid()).isTrue();
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldFailForMalformedTurtle() throws IOException {
        Path ttlFile = tempDir.resolve("malformed.ttl");
        Files.writeString(ttlFile, """
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .

                <http://example.org/vocab/1>
                    a skos:ConceptScheme
                    dct:title "Missing dot and prefix"@it .
                """);

        RdfSyntaxValidationResult result = validator.validateTurtle(ttlFile.toString());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrorsSummary()).isNotBlank();
    }

    @Test
    void shouldFailForCompletelyInvalidContent() throws IOException {
        Path ttlFile = tempDir.resolve("garbage.ttl");
        Files.writeString(ttlFile, "this is not turtle at all { } [ ]");

        RdfSyntaxValidationResult result = validator.validateTurtle(ttlFile.toString());

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldFailForNonExistentFile() {
        RdfSyntaxValidationResult result = validator.validateTurtle("/non/existent/file.ttl");

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldReportLineAndColumnForErrors() throws IOException {
        Path ttlFile = tempDir.resolve("error_position.ttl");
        Files.writeString(ttlFile, """
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .

                <http://example.org/vocab/1>
                    a skos:ConceptScheme ;
                    skos:prefLabel "unclosed string@it .
                """);

        RdfSyntaxValidationResult result = validator.validateTurtle(ttlFile.toString());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().get(0).getLine()).isGreaterThan(0);
    }

    @Test
    void shouldPassForEmptyButValidTurtle() throws IOException {
        Path ttlFile = tempDir.resolve("empty.ttl");
        Files.writeString(ttlFile, "# This is an empty turtle file with only a comment\n");

        RdfSyntaxValidationResult result = validator.validateTurtle(ttlFile.toString());

        assertThat(result.isValid()).isTrue();
    }
}
