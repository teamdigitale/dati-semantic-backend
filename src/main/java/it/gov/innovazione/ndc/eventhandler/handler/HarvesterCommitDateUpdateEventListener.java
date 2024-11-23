package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.eventhandler.NdcEventHandler;
import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterUpdateCommitDateEvent;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class HarvesterCommitDateUpdateEventListener implements NdcEventHandler {

    private static final Collection<Class<?>> SUPPORTED_EVENTS = List.of(HarvesterUpdateCommitDateEvent.class);

    private final HarvesterRunService harvesterRunService;

    @Override
    public boolean canHandle(NdcEventWrapper<?> event) {
        return SUPPORTED_EVENTS.contains(event.getPayload().getClass());
    }

    @Override
    public void handle(NdcEventWrapper<?> event) {
        HarvesterUpdateCommitDateEvent payload = (HarvesterUpdateCommitDateEvent) event.getPayload();
        if (Objects.isNull(payload) || Objects.isNull(payload.getCommitDate())) {
            log.warn("Received invalid HarvesterUpdateCommitDateEvent: {}", payload);
            return;
        }
        harvesterRunService.getAllRuns().stream()
                .filter(harvesterRun -> payload.getRunId().equals(harvesterRun.getId()))
                .findAny()
                .map(harvesterRun -> harvesterRun.withRevisionCommittedAt(payload.getCommitDate()))
                .ifPresent(harvesterRunService::updateHarvesterRunCommittedAt);
    }
}
