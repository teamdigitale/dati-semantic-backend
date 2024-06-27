package it.gov.innovazione.ndc.eventhandler.handler;

import it.gov.innovazione.ndc.alerter.data.ProfileService;
import it.gov.innovazione.ndc.eventhandler.NdcEventHandler;
import it.gov.innovazione.ndc.eventhandler.NdcEventWrapper;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.ConfigKey;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlerterEnabledEventListener implements NdcEventHandler {

    private static final Collection<Class<?>> SUPPORTED_EVENTS = List.of(ConfigService.ConfigEvent.class);

    private final ProfileService profileService;

    @Value("${alerter.mail-sender.backoff:PT1H}")
    private Duration backoff;

    @Override
    public boolean canHandle(NdcEventWrapper<?> event) {
        return SUPPORTED_EVENTS.contains(event.getPayload().getClass());
    }

    @Override
    public void handle(NdcEventWrapper<?> event) {
        ConfigService.ConfigEvent payload = (ConfigService.ConfigEvent) event.getPayload();
        if (Objects.isNull(payload) || Objects.isNull(payload.getChanges())) {
            return;
        }
        if (payload.isChange(ConfigKey.ALERTER_ENABLED, Boolean.TRUE)) {
            log.info("Alerter enabled, setting all profiles last updated");
            profileService.setAllLastUpdated(backoff);
        }
    }
}
