package it.gov.innovazione.ndc.eventhandler;

import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.ConfigKey.ALERTER_ENABLED;

@Service
@RequiredArgsConstructor
@Slf4j
public class NdcEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ConfigService configService;

    public <T> void publishEvent(
            String source,
            String type,
            String correlationId,
            String user,
            T event) {
        if (event.getClass().isAssignableFrom(AlertableEvent.class)) {
            log.warn("Event is not alertable, publishAlertableEvent should be used instead");
        }
        try {
            applicationEventPublisher.publishEvent(
                    NdcEventWrapper.<T>builder()
                            .source(source)
                            .type(type)
                            .correlationId(correlationId)
                            .timestamp(Instant.now())
                            .user(user)
                            .payload(event)
                            .build());
        } catch (Exception e) {
            log.error("Error publishing event", e);
        }
    }

    public <T extends AlertableEvent> void publishAlertableEvent(
            String source, T event) {
        Optional<Boolean> optAlerterEnabled = configService.fromGlobal(ALERTER_ENABLED);
        boolean alerterEnabled = optAlerterEnabled.orElse(false);

        if (!alerterEnabled) {
            log.warn("Alerter is disabled, event not published");
            return;
        }
        try {
            applicationEventPublisher.publishEvent(
                    NdcEventWrapper.<T>builder()
                            .source(source)
                            .type(event.getName())
                            .correlationId("")
                            .timestamp(Instant.now())
                            .payload(event)
                            .build());
        } catch (Exception e) {
            log.error("Error publishing alertable event", e);
        }
    }
}
