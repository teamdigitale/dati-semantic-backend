package it.gov.innovazione.ndc.eventhandler.event;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.harvester.service.ActualConfigService;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.eclipse.jgit.util.StringUtils.equalsIgnoreCase;

@Slf4j
public abstract class ConfigService {

    public <T> Optional<T> fromGlobal(ActualConfigService.ConfigKey key) {
        try {
            Class<T> type = null;
            return Optional.ofNullable(getNdcConfiguration())
                    .map(NdcConfiguration::getValue)
                    .map(value -> value.get(key))
                    .map(ConfigEntry::getValue)
                    .map(value -> safelyParse(value, key, type));
        } catch (Exception e) {
            log.error("Error reading global configuration", e);
            return Optional.empty();
        }
    }

    private <T> Optional<T> fromRepo(ActualConfigService.ConfigKey key, String repoId) {
        try {
            Class<T> type = null;

            return Optional.ofNullable(getRepoConfiguration(repoId))
                    .map(NdcConfiguration::getValue)
                    .map(value -> value.get(key))
                    .map(ConfigEntry::getValue)
                    .map(value -> safelyParse(value, key, type));
        } catch (Exception e) {
            log.error("Error reading configuration for repo [{}]", repoId, e);
            return Optional.empty();
        }
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
            return (T) key.getParser().getParsingFunction().apply(value.toString());
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
    @Getter
    public static class ConfigEvent implements AlertableEvent {
        private final Map<ActualConfigService.ConfigKey, ConfigChange> changes;
        private final String destination;
        private final Exception error;

        public boolean isChangeKey(ActualConfigService.ConfigKey key) {
            return Objects.nonNull(changes) && changes.containsKey(key);
        }

        public boolean isChange(ActualConfigService.ConfigKey key, Object newValue, Object oldValue) {
            try {
                return isChangeKey(key) && isNewValue(key, newValue) && isOldValue(key, oldValue);
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isNewValue(ActualConfigService.ConfigKey key, Object newValue) {
            return isValue(key, newValue, ConfigChange::getNewValue);
        }

        private Boolean isOldValue(ActualConfigService.ConfigKey key, Object oldValue) {
            return isValue(key, oldValue, ConfigChange::getOldValue);
        }

        private Boolean isValue(ActualConfigService.ConfigKey key,
                                Object value,
                                java.util.function.Function<ConfigChange, ConfigEntry> valueGetter) {
            return Optional.ofNullable(changes)
                    .map(c -> c.get(key))
                    .map(valueGetter)
                    .map(ConfigEntry::getValue)
                    .map(Object::toString)
                    .map(v -> equalsIgnoreCase(v, value.toString()))
                    .orElse(false);
        }

        @Override
        public String getName() {
            if (Objects.nonNull(error)) {
                return "Configuration update error: " + error.getMessage();
            }
            return "Configuration updated";
        }

        @Override
        public String getDescription() {
            if (Objects.nonNull(error)) {
                return "Error updating configuration for " + destination + ", cause: " + error.getMessage();
            }
            return "Configuration updated for " + destination;
        }

        @Override
        public EventCategory getCategory() {
            return EventCategory.APPLICATION;
        }

        @Override
        public Severity getSeverity() {
            if (Objects.nonNull(error)) {
                return Severity.ERROR;
            }
            return Severity.INFO;
        }

        @Override
        public Map<String, Object> getContext() {
            return changes.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        }
    }

    @Builder
    @Getter
    public static class ConfigChange {
        private final ConfigEntry oldValue;
        private final ConfigEntry newValue;
    }
}
