package it.gov.innovazione.ndc.service.logging;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Wire {@code harvester.logging.max-value-length} into {@link LoggingContext}'s static field.
 * {@link LoggingContext} e' una value class (@Data/@Builder), non puo' iniettare property
 * direttamente; questo bean fa da ponte all'avvio.
 */
@Configuration
@RequiredArgsConstructor
public class LoggingContextConfig {

    @Value("${harvester.logging.max-value-length:120}")
    private int maxValueLength;

    @PostConstruct
    void wire() {
        LoggingContext.setMaxValueLength(maxValueLength);
    }
}
