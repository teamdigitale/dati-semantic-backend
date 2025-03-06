package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.eventhandler.NdcEventHandler;
import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterFinishedEvent;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterStartedEvent;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.DashboardRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HarvesterRunUpdatingHandler implements NdcEventHandler {

    private static final Collection<Class<?>> SUPPORTED_EVENTS = Arrays.asList(
            HarvesterStartedEvent.class,
            HarvesterFinishedEvent.class);
    private final HarvesterRunService harvesterRunService;
    private final DashboardRepo dashboardRepo;

    @Override
    public boolean canHandle(NdcEventWrapper<?> event) {
        return SUPPORTED_EVENTS.contains(event.getPayload().getClass());
    }

    @Override
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

        int saved = harvesterRunService.updateHarvesterRun(harvesterRun);
        if (saved != 1) {
            log.error("*** HarvesterRun not updated: {}", harvesterRun);
        }
        log.info("invalidating stats cache");
        dashboardRepo.invalidateCache();
    }

    private void handleHarvesterStartedEvent(NdcEventWrapper<HarvesterStartedEvent> event) {
        HarvesterRun harvesterRun = HarvesterRun.builder()
                .id(event.getPayload().getRunId())
                .correlationId(event.getCorrelationId())
                .repositoryId(event.getPayload().getRepository().getId())
                .repositoryUrl(event.getPayload().getRepository().getUrl())
                .instance(event.getPayload().getInstance().name())
                .startedAt(event.getTimestamp())
                .startedBy(event.getUser())
                .revision(event.getPayload().getRevision())
                .status(HarvesterRun.Status.RUNNING)
                .build();
        int saved = harvesterRunService.saveHarvesterRun(harvesterRun);
        if (saved != 1) {
            log.error("*** HarvesterRun not saved: {}", harvesterRun);
        }
    }
}
