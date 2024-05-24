package it.gov.innovazione.ndc.eventhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@Slf4j
@RequiredArgsConstructor
public class NdcEventListener {

    private final Collection<NdcEventHandler> handlers;

    @EventListener(NdcEventWrapper.class)
    public void handleNdcEvent(NdcEventWrapper<?> event) {
        log.trace("Received event: {}", event);
        handlers.stream()
                .filter(handler -> handler.canHandle(event))
                .forEach(handler -> handleSafely(() -> handler.handle(event)));
    }

    private void handleSafely(Runnable handlerExecution) {
        try {
            handlerExecution.run();
        } catch (Exception e) {
            log.error("Error handling event", e);
        }
    }
}
