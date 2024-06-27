package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.alerter.AlerterService;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.eventhandler.NdcEventHandler;
import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlerterEventHandler implements NdcEventHandler {

    private static final Collection<Class<?>> SUPPORTED_EVENTS = List.of(AlertableEvent.class);
    private final AlerterService alerterService;

    @Override
    public boolean canHandle(NdcEventWrapper<?> event) {
        return SUPPORTED_EVENTS.stream()
                .anyMatch(supportedEvent -> supportedEvent.isAssignableFrom(event.getPayload().getClass()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(NdcEventWrapper<?> event) {
        if (event.getPayload() instanceof AlertableEvent) {
            NdcEventWrapper<AlertableEvent> alertableEvent = (NdcEventWrapper<AlertableEvent>) event;
            handleAlertableEvent(alertableEvent);
        }
    }

    private void handleAlertableEvent(NdcEventWrapper<AlertableEvent> alertableEvent) {
        alerterService.alert(alertableEvent.getPayload());
    }
}
