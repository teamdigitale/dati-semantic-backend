package it.gov.innovazione.ndc.harvester.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Primary
@RequiredArgsConstructor
public class CachingConfigService implements ConfigService {

    private final ActualConfigService actualConfigService;
    private NdcConfiguration ndcConfiguration;

    @Override
    public synchronized NdcConfiguration getNdcConfiguration() {
        if (ndcConfiguration == null) {
            ndcConfiguration = actualConfigService.getNdcConfiguration();
        }
        return ndcConfiguration;
    }

    @Override
    public synchronized void writeConfigKey(ActualConfigService.ConfigKey key, String writtenBy, Object value) {
        actualConfigService.writeConfigKey(key, writtenBy, value);
        ndcConfiguration = null;
    }

    @Override
    public synchronized void setNdConfig(Map<ActualConfigService.ConfigKey, Object> config, String writtenBy) {
        actualConfigService.setNdConfig(config, writtenBy);
        ndcConfiguration = null;
    }

    @Override
    public synchronized void removeConfigKey(ActualConfigService.ConfigKey configKey, String writtenBy) {
        actualConfigService.removeConfigKey(configKey, writtenBy);
        ndcConfiguration = null;
    }
}
