package it.gov.innovazione.ndc.eventhandler;

import it.gov.innovazione.ndc.harvester.HarvesterStartedEvent;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class HarvesterEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    public <T extends NdcEventWrapper.NdcEvent> void publishEvent(
            String source,
            String type,
            String correlationId,
            T event) {
        try {
            applicationEventPublisher.publishEvent(
                    NdcEventWrapper.<T>builder()
                            .source(source)
                            .type(type)
                            .correlationId(correlationId)
                            .timestamp(Instant.now())
                            .payload(event)
                            .build());
        } catch (Exception e) {
            log.error("Error publishing event", e);
        }
    }

    public void publishHarvesterStartedEvent(Repository repository, String correlationId, String revision, String runId) {
        publishEvent(
                "harvester",
                "harvester.started",
                correlationId,
                HarvesterStartedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .build());
    }

    public void publishHarvesterSuccessfulEvent(Repository repository, String correlationId, String revision, String runId) {
        publishEvent(
                "harvester",
                "harvester.finished.success",
                correlationId,
                HarvesterFinishedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .status(HarvesterFinishedEvent.Status.SUCCESS)
                        .build());
    }

    public void publishHarvesterFailedEvent(Repository repository, String correlationId, String revision, String runId, Exception e) {
        publishEvent(
                "harvester",
                "harvester.finished.failure",
                correlationId,
                HarvesterFinishedEvent.builder()
                        .runId(runId)
                        .repository(repository)
                        .revision(revision)
                        .status(HarvesterFinishedEvent.Status.FAILURE)
                        .exception(e)
                        .build());
    }
}
