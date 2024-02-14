package it.gov.innovazione.ndc.harvester.service;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ConfigService {
    default <T> T getParsedOrGetDefault(ActualConfigService.ConfigKey key, Supplier<T> defaultValue) {
        Class<T> type = null;
        return Optional.ofNullable(getNdcConfiguration())
                .map(NdcConfiguration::getValue)
                .map(value -> value.get(key))
                .map(ConfigEntry::getValue)
                .map(value -> safelyParse(value, key, type))
                .orElseGet(defaultValue);
    }

    @SuppressWarnings("unchecked")
    default <T> T safelyParse(Object value, ActualConfigService.ConfigKey key, Class<T> clazz) {
        try {
            return (T) key.getParser().getParser().apply(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    ActualConfigService.NdcConfiguration getNdcConfiguration();

    void writeConfigKey(ActualConfigService.ConfigKey key, String writtenBy, Object value);

    void setNdConfig(Map<ActualConfigService.ConfigKey, Object> config, String writtenBy);

    void removeConfigKey(ActualConfigService.ConfigKey configKey, String writtenBy);

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    class NdcConfiguration {
        private final Map<ActualConfigService.ConfigKey, ConfigEntry> value;

        public String get(ActualConfigService.ConfigKey key) {
            return String.valueOf(value.get(key));
        }
    }

    @Data
    @Builder
    class ConfigEntry {
        private final String writtenBy;
        private final Instant writtenAt;
        private final Object value;
    }

    @Builder
    class ConfigEvent {
        private final Map<ActualConfigService.ConfigKey, ConfigChange> changes;
        private final Exception error;
    }

    @Builder
    class ConfigChange {
        private final ConfigEntry oldValue;
        private final ConfigEntry newValue;
    }
}
