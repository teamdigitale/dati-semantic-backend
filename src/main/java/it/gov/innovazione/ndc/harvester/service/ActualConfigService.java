package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActualConfigService extends ConfigService {

    private static final String CONFIG_ID = "ndc";

    private final JdbcTemplate jdbcTemplate;
    private final NdcEventPublisher ndcEventPublisher;
    private final ConfigReaderService configReaderService;
    private final RepositoryService repositoryService;

    @Override
    public NdcConfiguration getNdcConfiguration() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM CONFIGURATION WHERE ID = ?",
                    new Object[]{CONFIG_ID},
                    (rs, rowNum) -> NdcConfiguration.of(configReaderService.toMap(rs.getString("VALUE"))));
        } catch (EmptyResultDataAccessException e) {
            log.warn("No configuration found in the database. Returning an empty configuration.");
            return NdcConfiguration.of(Map.of());
        } catch (Exception e) {
            log.warn("There was an error reading the configuration from the database. Returning an empty configuration.", e);
            return NdcConfiguration.of(Map.of());
        }
    }

    @Override
    public NdcConfiguration getRepoConfiguration(String repoId) {
        return repositoryService.findActiveRepoById(repoId)
                .map(Repository::getConfig)
                .map(NdcConfiguration::of)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));
    }

    @Override
    public void writeConfigKey(ConfigKey key, String writtenBy, Object value) {
        writeConfigKey(key, writtenBy, value,
                () -> getNdcConfiguration().getValue(),
                this::writeConfig,
                CONFIG_ID);
    }

    @Override
    public void writeConfigKey(ConfigKey key, String writtenBy, Object value, String repoId) {
        writeConfigKey(key, writtenBy, value,
                () -> getRepoConfiguration(repoId).getValue(),
                config -> writeConfig(config, repoId),
                repoId);
    }

    private void writeConfigKey(ConfigKey key, String writtenBy, Object value,
                                Supplier<Map<ConfigKey, ConfigEntry>> configSupplier,
                                Consumer<Map<ConfigKey, ConfigEntry>> configWriter,
                                String destination) {
        validateOne(key, value);
        ConfigEntry oldValueIfExist = null;
        try {
            Map<ConfigKey, ConfigEntry> config = new HashMap<>(configSupplier.get());

            if (config.containsKey(key)) {
                oldValueIfExist = config.get(key);
            }

            ConfigEntry newValue = ConfigEntry.builder()
                    .writtenBy(writtenBy)
                    .writtenAt(Instant.now())
                    .value(value)
                    .build();

            config.put(key, newValue);
            configWriter.accept(config);

            sendConfigWrittenEvent(
                    "write-config-key",
                    destination,
                    Map.of(key, ConfigChange.builder()
                            .oldValue(oldValueIfExist)
                            .newValue(newValue)
                            .build()), writtenBy);
        } catch (Exception e) {
            sendConfigWriteErrorEvent(
                    "write-config-key",
                    destination,
                    Map.of(key, ConfigChange.builder()
                            .oldValue(oldValueIfExist)
                            .build()), writtenBy, e);
            throw e;
        }

    }

    private void validate(Map<ConfigKey, Object> config) {
        config.forEach(this::validateOne);
    }

    private void validateOne(ConfigKey key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (!key.getValidator().getValidator().test(String.valueOf(value))) {
            throw new IllegalArgumentException("Value " + value + " is not valid for " + key + " key, using validator " + key.getValidator().name());
        }
    }

    private void sendConfigWrittenEvent(String operation, String destination, Map<ConfigKey, ConfigChange> changes, String writtenBy) {
        ndcEventPublisher.publishEvent("config", "config." + operation, null, writtenBy,
                ConfigEvent.builder()
                        .destination(destination)
                        .changes(changes)
                        .build());
    }

    private void sendConfigWriteErrorEvent(String operation, String destination, Map<ConfigKey, ConfigChange> changes, String writtenBy, Exception exception) {
        ndcEventPublisher.publishEvent("config", "config." + operation + ".error", null, writtenBy,
                ConfigEvent.builder()
                        .destination(destination)
                        .changes(changes)
                        .error(exception)
                        .build());
    }

    @Override
    public void setConfig(Map<ConfigKey, Object> config, String writtenBy) {
        setConfig(
                config,
                writtenBy,
                () -> getNdcConfiguration().getValue(),
                this::writeConfig,
                CONFIG_ID);
    }

    @Override
    public void setConfig(Map<ConfigKey, Object> config, String writtenBy, String repoId) {
        setConfig(
                config,
                writtenBy,
                () -> getRepoConfiguration(repoId).getValue(),
                conf -> writeConfig(conf, repoId),
                repoId);
    }

    private void setConfig(Map<ConfigKey, Object> config, String writtenBy,
                           Supplier<Map<ConfigKey, ConfigEntry>> configSupplier,
                           Consumer<Map<ConfigKey, ConfigEntry>> configWriter,
                           String destination) {
        validate(config);
        try {
            Map<ConfigKey, ConfigEntry> oldConfig = new HashMap<>(configSupplier.get());
            Map<ConfigKey, ConfigEntry> newConfig = config.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> ConfigEntry.builder()
                                    .writtenBy(writtenBy)
                                    .writtenAt(Instant.now())
                                    .value(entry.getValue())
                                    .build()));

            configWriter.accept(newConfig);

            Map<ConfigKey, ConfigChange> changes = Stream.concat(oldConfig.keySet().stream(), newConfig.keySet().stream())
                    .distinct()
                    .collect(Collectors.toMap(Function.identity(),
                            key -> ConfigChange.builder()
                                    .oldValue(oldConfig.get(key))
                                    .newValue(newConfig.get(key))
                                    .build()));

            sendConfigWrittenEvent("write-config", destination, changes, writtenBy);
        } catch (Exception e) {
            sendConfigWriteErrorEvent(
                    "write-config",
                    destination,
                    config.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> ConfigChange.builder()
                                    .oldValue(null)
                                    .build())),
                    writtenBy, e);
            throw e;
        }
    }

    @SneakyThrows
    private void writeConfig(Map<ConfigKey, ConfigEntry> config) {
        String valueAsString = configReaderService.fromMap(config);
        jdbcTemplate.update(
                "INSERT INTO CONFIGURATION (ID, VALUE) VALUES (?, ?) ON DUPLICATE KEY UPDATE VALUE = ?",
                CONFIG_ID, valueAsString, valueAsString);
    }

    @SneakyThrows
    private void writeConfig(Map<ConfigKey, ConfigEntry> config, String repoId) {
        String valueAsString = configReaderService.fromMap(config);
        jdbcTemplate.update("UPDATE REPOSITORY SET CONFIG = ? WHERE ID = ?", valueAsString, repoId);
    }

    @Override
    public void removeConfigKey(ConfigKey configKey, String writtenBy) {
        removeConfigKey(configKey, writtenBy,
                () -> getNdcConfiguration().getValue(),
                this::writeConfig,
                CONFIG_ID);
    }

    @Override
    public void removeConfigKey(ConfigKey configKey, String writtenBy, String repoId) {
        removeConfigKey(configKey, writtenBy,
                () -> getRepoConfiguration(repoId).getValue(),
                config -> writeConfig(config, repoId),
                repoId);
    }

    private void removeConfigKey(ConfigKey configKey, String writtenBy,
                                 Supplier<Map<ConfigKey, ConfigEntry>> configSupplier,
                                 Consumer<Map<ConfigKey, ConfigEntry>> configWriter,
                                 String destination) {
        ConfigEntry oldValue = null;
        try {
            Map<ConfigKey, ConfigEntry> config = new HashMap<>(configSupplier.get());
            if (config.containsKey(configKey)) {
                oldValue = config.get(configKey);
            }
            config.remove(configKey);
            configWriter.accept(config);
            sendConfigWrittenEvent(
                    "remove-config-key",
                    destination,
                    Map.of(configKey, ConfigChange.builder()
                            .oldValue(oldValue)
                            .build()), writtenBy);
        } catch (Exception e) {
            sendConfigWriteErrorEvent(
                    "remove-config-key",
                    destination,
                    Map.of(configKey, ConfigChange.builder()
                            .oldValue(oldValue)
                            .build()), writtenBy, e);
            throw e;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum ConfigKey {
        MAX_FILE_SIZE_BYTES("The maximum file size in bytes of a file to be harvested", Validator.IS_LONG, Parser.TO_LONG),
        GITHUB_ISSUER_ENABLED("Enable the GitHub issuer capability", Validator.IS_BOOLEAN, Parser.TO_BOOLEAN),
        ALERTER_ENABLED("Enable the Alerter capability", Validator.IS_BOOLEAN, Parser.TO_BOOLEAN);

        private final String description;
        private final Validator validator;
        private final Parser parser;
    }

    @Getter
    @RequiredArgsConstructor
    private enum Validator {
        IS_LONG(Parser.TO_LONG),
        IS_BOOLEAN(Parser.TO_BOOLEAN);

        private final Parser parser;

        public Predicate<String> getValidator() {
            return s -> {
                try {
                    parser.parser.apply(s);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            };
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum Parser {
        TO_LONG(Long::parseLong),
        TO_BOOLEAN(Boolean::parseBoolean);
        private final Function<String, Object> parser;
    }    
}
