package it.gov.innovazione.ndc.alerter;

import it.gov.innovazione.ndc.alerter.data.EventService;
import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;

import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logApplicationInfo;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Service
@RequiredArgsConstructor
public class AlerterService {

    private final EventService eventService;

    public void alert(AlertableEvent alertableEvent) {
        eventService.create(EventDto.builder()
                .name(alertableEvent.getName())
                .description(alertableEvent.getDescription())
                .category(alertableEvent.getCategory())
                .context(alertableEvent.getContext())
                .severity(alertableEvent.getSeverity())
                .createdBy(getUser())
                .occurredAt(defaultIfNull(alertableEvent.getOccurredAt(), Instant.now()))
                .build());
        logApplicationInfo(LoggingContext.builder()
                .component("alerter")
                .message("Alerted event")
                .details(alertableEvent.getDescription())
                .eventCategory(alertableEvent.getCategory())
                .build());
    }

    public static String getUser() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName)
                .orElse("system");
    }

}
