package it.gov.innovazione.ndc.alerter;

import it.gov.innovazione.ndc.alerter.data.EventService;
import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

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
    }

    private String getUser() {
        return defaultIfNull(SecurityContextHolder.getContext().getAuthentication().getName(), "system");

    }

}
