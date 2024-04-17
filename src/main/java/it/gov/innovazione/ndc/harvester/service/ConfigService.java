package it.gov.innovazione.ndc.harvester.service;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class ConfigService {

    private <T> Optional<T> fromGlobal(ActualConfigService.ConfigKey key) {
        Class<T> type = null;
        return Optional.ofNullable(getNdcConfiguration())
                .map(NdcConfiguration::getValue)
                .map(value -> value.get(key))
                .map(ConfigEntry::getValue)
                .map(value -> safelyParse(value, key, type));
    }

    private <T> Optional<T> fromRepo(ActualConfigService.ConfigKey key, String repoId) {
        Class<T> type = null;
        return Optional.ofNullable(getRepoConfiguration(repoId))
                .map(NdcConfiguration::getValue)
                .map(value -> value.get(key))
                .map(ConfigEntry::getValue)
                .map(value -> safelyParse(value, key, type));
    }

    public <T> T getFromRepoOrGlobalOrDefault(
            ActualConfigService.ConfigKey key,
            String repoId,
            T defaultValue) {
        Optional<T> repoValue = fromRepo(key, repoId);
        if (repoValue.isPresent()) {
            log.info("using {}={} from repository [{}] configuration", key, repoValue.get(), repoId);
            return repoValue.get();
        }
        Optional<T> configValue = fromGlobal(key);
        if (configValue.isPresent()) {
            log.info("using {}={} from global configuration", key, configValue.get());
            return configValue.get();
        }
        log.info("no {} found in repo configuration nor in global configuration, using defaultValue {}", key, defaultValue);

        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    <T> T safelyParse(Object value, ActualConfigService.ConfigKey key, Class<T> clazz) {
        try {
            return (T) key.getParser().getParser().apply(value.toString());
        } catch (Exception e) {
            log.warn("Error parsing value for key {} with value \"{}\"", key, value, e);
            return null;
        }
    }

    public abstract ConfigService.NdcConfiguration getNdcConfiguration();

    public abstract NdcConfiguration getRepoConfiguration(String repoId);

    public abstract void writeConfigKey(ActualConfigService.ConfigKey key, String writtenBy, Object value);

    public abstract void writeConfigKey(ActualConfigService.ConfigKey key, String writtenBy, Object value, String repoId);

    public abstract void setConfig(Map<ActualConfigService.ConfigKey, Object> config, String writtenBy);

    public abstract void setConfig(Map<ActualConfigService.ConfigKey, Object> config, String writtenBy, String repoId);

    public abstract void removeConfigKey(ActualConfigService.ConfigKey configKey, String writtenBy);

    public abstract void removeConfigKey(ActualConfigService.ConfigKey configKey, String writtenBy, String repoId);

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    public static class NdcConfiguration {
        private final Map<ActualConfigService.ConfigKey, ConfigEntry> value;

        public String get(ActualConfigService.ConfigKey key) {
            return String.valueOf(value.get(key));
        }
    }

    @Data
    @Builder
    public static class ConfigEntry {
        private final String writtenBy;
        private final Instant writtenAt;
        private final Object value;
    }

    @Builder
    public static class ConfigEvent {
        private final Map<ActualConfigService.ConfigKey, ConfigChange> changes;
        private final String destination;
        private final Exception error;
    }

    @Builder
    public static class ConfigChange {
        private final ConfigEntry oldValue;
        private final ConfigEntry newValue;
    }
}
