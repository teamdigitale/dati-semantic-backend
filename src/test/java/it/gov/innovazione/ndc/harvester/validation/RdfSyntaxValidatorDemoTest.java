package it.gov.innovazione.ndc.harvester.validation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demo test per GOV-QUAL: Validazione Sintattica RDF (Turtle).
 *
 * <p>Esegui con:
 *   ./gradlew test --tests "*.RdfSyntaxValidatorDemoTest" --info
 *
 * <p>Usa i file TTL di esempio in src/test/resources/demo-sample-ttl/
 * (copia di backlog/GOV-QUAL/demo/sample-ttl/)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RdfSyntaxValidatorDemoTest {

    private static final String SAMPLE_RESOURCE_DIR = "demo-sample-ttl";
    private static RdfSyntaxValidator validator;

    @BeforeAll
    static void setup() {
        validator = new RdfSyntaxValidator();
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("  GOV-QUAL DEMO: Validazione Sintattica Turtle (pre-harvesting)");
        System.out.println("=".repeat(80));
        System.out.println();
    }

    @Test
    @Order(1)
    void demo_01_fileValido() {
        Path file = resolve("valid-ontology.ttl");
        RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());

        printResult("valid-ontology.ttl", result);

        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    @Order(2)
    void demo_02_errore_puntoMancante() {
        Path file = resolve("error-missing-dot.ttl");
        RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());

        printResult("error-missing-dot.ttl", result);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @Order(3)
    void demo_03_errore_prefissoMalformato() {
        Path file = resolve("error-bad-prefix.ttl");
        RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());

        printResult("error-bad-prefix.ttl", result);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @Order(4)
    void demo_04_errore_stringaNonChiusa() {
        Path file = resolve("error-unclosed-string.ttl");
        RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());

        printResult("error-unclosed-string.ttl", result);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @Order(5)
    void demo_05_errore_iriConSpazi() {
        Path file = resolve("error-bad-iri.ttl");
        RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());

        printResult("error-bad-iri.ttl", result);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @Order(6)
    void demo_06_errore_fileNonTurtle() {
        Path file = resolve("error-garbage.ttl");
        RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());

        printResult("error-garbage.ttl", result);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @Order(7)
    void demo_07_errore_problemiMultipli() {
        Path file = resolve("error-multiple-issues.ttl");
        RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());

        printResult("error-multiple-issues.ttl", result);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @Order(8)
    void demo_08_riepilogoSuTuttiIFile() throws IOException {
        System.out.println();
        System.out.println("-".repeat(80));
        System.out.println("  RIEPILOGO: scansione di tutti i file nella directory demo");
        System.out.println("-".repeat(80));

        Path dir = resolveDir();

        int valid = 0;
        int invalid = 0;

        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".ttl")).sorted().toList()) {
                RdfSyntaxValidationResult result = validator.validateTurtle(file.toString());
                String status = result.isValid() ? "PASS" : "FAIL";
                String detail = result.isValid()
                        ? (result.hasWarnings() ? result.getWarnings().size() + " warning(s)" : "OK")
                        : result.getErrors().size() + " errore/i";

                System.out.printf("  [%s] %-35s %s%n", status, file.getFileName(), detail);

                if (result.isValid()) {
                    valid++;
                } else {
                    invalid++;
                }
            }
        }

        System.out.println();
        System.out.printf("  Totale: %d file | %d validi | %d con errori bloccanti%n", valid + invalid, valid, invalid);
        System.out.println("=".repeat(80));
        System.out.println();

        assertThat(valid).isGreaterThan(0);
        assertThat(invalid).isGreaterThan(0);
    }

    // --- utility ---

    private static Path resolveDir() {
        try {
            return Paths.get(Objects.requireNonNull(
                    RdfSyntaxValidatorDemoTest.class.getClassLoader().getResource(SAMPLE_RESOURCE_DIR)).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path resolve(String filename) {
        Path file = resolveDir().resolve(filename);
        assertThat(file).as("Il file di esempio %s deve esistere", filename).exists();
        return file;
    }

    private static void printResult(String filename, RdfSyntaxValidationResult result) {
        System.out.println("-".repeat(80));
        System.out.printf("  File: %s%n", filename);
        System.out.printf("  Esito: %s%n", result.isValid() ? "VALIDO" : "ERRORE BLOCCANTE");

        if (result.hasErrors()) {
            System.out.println("  Errori:");
            result.getErrors().forEach(e ->
                    System.out.printf("    - %s%n", e));
        }
        if (result.hasWarnings()) {
            System.out.println("  Warning:");
            result.getWarnings().forEach(w ->
                    System.out.printf("    - %s%n", w));
        }
        if (!result.hasErrors() && !result.hasWarnings()) {
            System.out.println("  Nessun problema rilevato.");
        }
        System.out.println();
    }
}
