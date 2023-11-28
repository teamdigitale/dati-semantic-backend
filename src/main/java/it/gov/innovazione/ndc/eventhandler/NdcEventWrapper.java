package it.gov.innovazione.ndc.eventhandler;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
public class NdcEventWrapper<T extends NdcEventWrapper.NdcEvent> {
    private final String source;
    private final String type;
    private final String correlationId;
    private final Instant timestamp;
    private final T payload;

    public interface NdcEvent {
    }
}
