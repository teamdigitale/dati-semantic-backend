package it.gov.innovazione.ndc.alerter.event;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Getter
@Builder
public class DefaultAlertableEvent implements AlertableEvent {
    private final String name;
    private final String description;
    @Builder.Default
    private final String createdBy = Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .map(Principal::getName)
            .orElse("system");
    private final Instant occurredAt;
    private final EventCategory category;
    private final Severity severity;
    private final Map<String, Object> context;
}
