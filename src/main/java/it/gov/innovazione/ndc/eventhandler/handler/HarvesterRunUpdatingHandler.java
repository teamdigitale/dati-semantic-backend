package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.eventhandler.NdcEventHandler;
import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterFinishedEvent;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterStartedEvent;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
@RequiredArgsConstructor
public class HarvesterRunUpdatingHandler implements NdcEventHandler {

    private final RepositoryService repositoryService;
    private final HarvesterRunService harvesterRunService;

    private static final Collection<Class<?>> SUPPORTED_EVENTS = Arrays.asList(
            HarvesterStartedEvent.class,
            HarvesterFinishedEvent.class);

    @Override
    public boolean canHandle(NdcEventWrapper<?> event) {
        return SUPPORTED_EVENTS.contains(event.getPayload().getClass());
    }

    @Override
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(NdcEventWrapper<?> event) {
        if (event.getPayload() instanceof HarvesterStartedEvent) {
            NdcEventWrapper<HarvesterStartedEvent> harvesterStartedEvent = (NdcEventWrapper<HarvesterStartedEvent>) event;
            handleHarvesterStartedEvent(harvesterStartedEvent);
        } else if (event.getPayload() instanceof HarvesterFinishedEvent) {
            NdcEventWrapper<HarvesterFinishedEvent> harvesterFinishedEvent = (NdcEventWrapper<HarvesterFinishedEvent>) event;
            handleHarvesterFinishedEvent(harvesterFinishedEvent);
        }
    }

    private void handleHarvesterFinishedEvent(NdcEventWrapper<HarvesterFinishedEvent> event) {
        HarvesterRun harvesterRun = HarvesterRun.builder()
                .id(event.getPayload().getRunId())
                .endedAt(event.getTimestamp())
                .status(event.getPayload().getStatus())
                .reason(Optional.of(event)
                        .map(NdcEventWrapper::getPayload)
                        .map(HarvesterFinishedEvent::getException)
                        .map(Throwable::getMessage)
                        .orElse(""))
                .build();

        harvesterRunService.updateHarvesterRun(harvesterRun);
    }

    private void handleHarvesterStartedEvent(NdcEventWrapper<HarvesterStartedEvent> event) {
        HarvesterRun harvesterRun = HarvesterRun.builder()
                .id(event.getPayload().getRunId())
                .correlationId(event.getCorrelationId())
                .repositoryId(event.getPayload().getRepository().getId())
                .repositoryUrl(event.getPayload().getRepository().getUrl())
                .startedAt(event.getTimestamp())
                .startedBy(event.getUser())
                .revision(event.getPayload().getRevision())
                .status(HarvesterRun.Status.RUNNING)
                .build();
        harvesterRunService.saveHarvesterRun(harvesterRun);
    }
}
