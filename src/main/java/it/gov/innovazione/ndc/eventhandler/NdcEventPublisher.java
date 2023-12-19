package it.gov.innovazione.ndc.eventhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NdcEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    public <T> void publishEvent(
            String source,
            String type,
            String correlationId,
            String user,
            T event) {
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
}
