package it.gov.innovazione.ndc.harvester.validation;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDFLib;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RdfSyntaxValidator {

    public RdfSyntaxValidationResult validateTurtle(String filePath) {
        log.debug("Validating Turtle syntax for {}", filePath);
        CollectingErrorHandler errorHandler = new CollectingErrorHandler();
        try {
            RDFParser.source(filePath)
                    .lang(Lang.TURTLE)
                    .errorHandler(errorHandler)
                    .checking(true)
                    .parse(StreamRDFLib.sinkNull());
        } catch (Exception e) {
            if (errorHandler.errors.isEmpty()) {
                errorHandler.errors.add(
                        RdfSyntaxValidationResult.Issue.builder()
                                .message(e.getMessage())
                                .build());
            }
        }

        RdfSyntaxValidationResult result = RdfSyntaxValidationResult.builder()
                .errors(errorHandler.errors)
                .warnings(errorHandler.warnings)
                .build();

        if (result.hasErrors()) {
            log.debug("Turtle syntax validation FAILED for {}: {} error(s), {} warning(s)",
                    filePath, result.getErrors().size(), result.getWarnings().size());
        } else {
            log.debug("Turtle syntax validation OK for {} ({} warning(s))",
                    filePath, result.getWarnings().size());
        }
        return result;
    }

    private static class CollectingErrorHandler implements ErrorHandler {

        private final List<RdfSyntaxValidationResult.Issue> errors = new ArrayList<>();
        private final List<RdfSyntaxValidationResult.Issue> warnings = new ArrayList<>();

        @Override
        public void warning(String message, long line, long col) {
            warnings.add(RdfSyntaxValidationResult.Issue.builder()
                    .line(line)
                    .col(col)
                    .message(message)
                    .build());
        }

        @Override
        public void error(String message, long line, long col) {
            errors.add(RdfSyntaxValidationResult.Issue.builder()
                    .line(line)
                    .col(col)
                    .message(message)
                    .build());
        }

        @Override
        public void fatal(String message, long line, long col) {
            errors.add(RdfSyntaxValidationResult.Issue.builder()
                    .line(line)
                    .col(col)
                    .message(message)
                    .build());
            throw new RdfSyntaxFatalException(message, line, col);
        }

        public List<RdfSyntaxValidationResult.Issue> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<RdfSyntaxValidationResult.Issue> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }
    }

    static class RdfSyntaxFatalException extends RuntimeException {
        RdfSyntaxFatalException(String message, long line, long col) {
            super(String.format("[line %d, col %d] %s", line, col, message));
        }
    }
}
