package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.eventhandler.NdcEventHandler;
import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import it.gov.innovazione.ndc.eventhandler.event.HarvesterFinishedEvent;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.audit.SemanticDeltaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDeltaHandler implements NdcEventHandler {

    private final SemanticDeltaService semanticDeltaService;

    @Override
    public boolean canHandle(NdcEventWrapper<?> event) {
        if (!(event.getPayload() instanceof HarvesterFinishedEvent finished)) {
            return false;
        }
        return finished.getStatus() == HarvesterRun.Status.SUCCESS;
    }

    @Override
    public void handle(NdcEventWrapper<?> event) {
        HarvesterFinishedEvent finished = (HarvesterFinishedEvent) event.getPayload();
        try {
            semanticDeltaService.computeAndPersistDelta(finished.getRepository(), finished.getRunId());
        } catch (Exception e) {
            log.error("Failed to compute semantic delta for run {}: {}", finished.getRunId(), e.getMessage(), e);
        }
    }
}
