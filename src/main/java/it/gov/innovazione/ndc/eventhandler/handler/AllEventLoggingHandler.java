package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.eventhandler.NdcEventHandler;
import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AllEventLoggingHandler implements NdcEventHandler {
    @Override
    public boolean canHandle(NdcEventWrapper<? extends NdcEventWrapper.NdcEvent> event) {
        return true;
    }

    @Override
    public void handle(NdcEventWrapper<? extends NdcEventWrapper.NdcEvent> event) {
        log.info("Received event: {}", event);
    }
}
