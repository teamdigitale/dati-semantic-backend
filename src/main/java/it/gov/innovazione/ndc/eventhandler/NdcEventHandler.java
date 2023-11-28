package it.gov.innovazione.ndc.eventhandler;

public interface NdcEventHandler {
    boolean canHandle(NdcEventWrapper<? extends NdcEventWrapper.NdcEvent> event);

    void handle(NdcEventWrapper<? extends NdcEventWrapper.NdcEvent> event);
}
