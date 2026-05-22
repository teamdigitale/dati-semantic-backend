package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterFinishedEvent;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterStartedEvent;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.service.audit.SemanticDeltaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResourceDeltaHandlerTest {

    @Mock
    private SemanticDeltaService deltaService;

    @InjectMocks
    private ResourceDeltaHandler handler;

    @Test
    void canHandleOnlySuccessFinishedEvents() {
        assertThat(handler.canHandle(wrap(finishedWithStatus(HarvesterRun.Status.SUCCESS)))).isTrue();
        assertThat(handler.canHandle(wrap(finishedWithStatus(HarvesterRun.Status.UNCHANGED)))).isFalse();
        assertThat(handler.canHandle(wrap(finishedWithStatus(HarvesterRun.Status.FAILURE)))).isFalse();
        assertThat(handler.canHandle(wrap(finishedWithStatus(HarvesterRun.Status.ALREADY_RUNNING)))).isFalse();
        assertThat(handler.canHandle(wrap(HarvesterStartedEvent.builder().build()))).isFalse();
    }

    @Test
    void successDispatchesToDeltaService() {
        Repository repo = Repository.builder().id("r1").url("https://example.org/repo").build();
        HarvesterFinishedEvent event = HarvesterFinishedEvent.builder()
                .runId("run-123")
                .repository(repo)
                .revision("abc")
                .status(HarvesterRun.Status.SUCCESS)
                .build();

        handler.handle(wrap(event));

        verify(deltaService).computeAndPersistDelta(eq(repo), eq("run-123"));
    }

    @Test
    void deltaServiceExceptionIsSwallowedSafely() {
        Repository repo = Repository.builder().id("r1").url("https://example.org/repo").build();
        HarvesterFinishedEvent event = HarvesterFinishedEvent.builder()
                .runId("run-x")
                .repository(repo)
                .status(HarvesterRun.Status.SUCCESS)
                .build();
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(deltaService).computeAndPersistDelta(any(), any());

        // must not propagate
        handler.handle(wrap(event));
        verify(deltaService).computeAndPersistDelta(eq(repo), eq("run-x"));
    }

    private static HarvesterFinishedEvent finishedWithStatus(HarvesterRun.Status status) {
        return HarvesterFinishedEvent.builder()
                .runId("r")
                .status(status)
                .build();
    }

    private static <T> NdcEventWrapper<T> wrap(T payload) {
        return NdcEventWrapper.<T>builder()
                .source("test")
                .type("test")
                .correlationId("c")
                .timestamp(Instant.now())
                .user("u")
                .payload(payload)
                .build();
    }
}
