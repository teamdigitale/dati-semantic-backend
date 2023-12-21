package it.gov.innovazione.ndc.eventhandler;

public interface NdcEventHandler {
    boolean canHandle(NdcEventWrapper<?> event);

    void handle(NdcEventWrapper<?> event);
}
