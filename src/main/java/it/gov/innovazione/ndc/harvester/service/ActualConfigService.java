package it.gov.innovazione.ndc.harvester.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ActualConfigService extends ConfigService {

    private static final String CONFIG_ID = "ndc";
    private static final TypeReference<Map<ConfigKey, ConfigEntry>> TYPE_REF = new TypeReference<>() {
    };
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final NdcEventPublisher ndcEventPublisher;

    @Override
    public NdcConfiguration getNdcConfiguration() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM CONFIGURATION WHERE ID = ?",
                    new Object[]{CONFIG_ID},
                    (rs, rowNum) -> NdcConfiguration.of(readSafely(rs.getString("VALUE"))));
        } catch (Exception e) {
            return NdcConfiguration.of(Map.of());
        }
    }

    @Override
    public void writeConfigKey(ConfigKey key, String writtenBy, Object value) {
        validateOne(key, value);
        ConfigEntry oldValueIfExist = null;
        try {
            Map<ConfigKey, ConfigEntry> config = new HashMap<>(getNdcConfiguration().getValue());

            if (config.containsKey(key)) {
                oldValueIfExist = config.get(key);
            }

            ConfigEntry newValue = ConfigEntry.builder()
                    .writtenBy(writtenBy)
                    .writtenAt(Instant.now())
                    .value(value)
                    .build();

            config.put(key, newValue);
            writeConfig(config);
            sendConfigWrittenEvent(
                    "write-config-key",
                    Map.of(key, ConfigChange.builder()
                            .oldValue(oldValueIfExist)
                            .newValue(newValue)
                            .build()), writtenBy);
        } catch (Exception e) {
            sendConfigWriteErrorEvent(
                    "write-config-key",
                    Map.of(key, ConfigChange.builder()
                            .oldValue(oldValueIfExist)
                            .build()), writtenBy, e);
        }

    }

    private void validate(Map<ConfigKey, Object> config) {
        config.forEach(this::validateOne);
    }

    private void validateOne(ConfigKey key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (!key.getValidator().validator.test(String.valueOf(value))) {
            throw new IllegalArgumentException("Value " + value + " is not valid for " + key + " key, using validator " + key.getValidator().name());
        }
    }

    private void sendConfigWrittenEvent(String operation, Map<ConfigKey, ConfigChange> changes, String writtenBy) {
        ndcEventPublisher.publishEvent("config", "config." + operation, null, writtenBy,
                ConfigEvent.builder()
                        .changes(changes)
                        .build());
    }

    private void sendConfigWriteErrorEvent(String operation, Map<ConfigKey, ConfigChange> changes, String writtenBy, Exception exception) {
        ndcEventPublisher.publishEvent("config", "config." + operation + ".error", null, writtenBy,
                ConfigEvent.builder()
                        .changes(changes)
                        .error(exception)
                        .build());
    }

    @Override
    public void setNdConfig(Map<ConfigKey, Object> config, String writtenBy) {
        validate(config);
        try {
            Map<ConfigKey, ConfigEntry> oldConfig = getNdcConfiguration().getValue();
            Map<ConfigKey, ConfigEntry> newConfig = config.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> ConfigEntry.builder()
                                    .writtenBy(writtenBy)
                                    .writtenAt(Instant.now())
                                    .value(entry.getValue())
                                    .build()));
            writeConfig(newConfig);

            Map<ConfigKey, ConfigChange> changes = Stream.concat(oldConfig.keySet().stream(), newConfig.keySet().stream())
                    .distinct()
                    .collect(Collectors.toMap(Function.identity(),
                            key -> ConfigChange.builder()
                                    .oldValue(oldConfig.get(key))
                                    .newValue(newConfig.get(key))
                                    .build()));

            sendConfigWrittenEvent("write-config", changes, writtenBy);
        } catch (Exception e) {
            sendConfigWriteErrorEvent(
                    "write-config",
                    config.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> ConfigChange.builder()
                                    .oldValue(null)
                                    .build())),
                    writtenBy, e);
        }
    }

    @SneakyThrows
    private void writeConfig(Map<ConfigKey, ConfigEntry> config) {
        String valueAsString = objectMapper.writeValueAsString(config);
        jdbcTemplate.update(
                "INSERT INTO CONFIGURATION (ID, VALUE) VALUES (?, ?) ON DUPLICATE KEY UPDATE VALUE = ?",
                CONFIG_ID, valueAsString, valueAsString);
    }

    @SneakyThrows
    private Map<ConfigKey, ConfigEntry> readSafely(String value) {
        try {
            return objectMapper.readValue(value, TYPE_REF);
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Override
    public void removeConfigKey(ConfigKey configKey, String writtenBy) {
        ConfigEntry oldValue = null;
        try {
            Map<ConfigKey, ConfigEntry> config = new EnumMap<>(getNdcConfiguration().getValue());
            if (config.containsKey(configKey)) {
                oldValue = config.get(configKey);
            }
            config.remove(configKey);
            writeConfig(config);
            sendConfigWrittenEvent(
                    "remove-config-key",
                    Map.of(configKey, ConfigChange.builder()
                            .oldValue(oldValue)
                            .build()), writtenBy);
        } catch (Exception e) {
            sendConfigWriteErrorEvent(
                    "remove-config-key",
                    Map.of(configKey, ConfigChange.builder()
                            .oldValue(oldValue)
                            .build()), writtenBy, e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum ConfigKey {
        MAX_FILE_SIZE_BYTES("The maximum file size in bytes of a file to be harvested", Validator.IS_LONG, Parser.TO_LONG),;

        private final String description;
        private final Validator validator;
        private final Parser parser;
    }

    @Getter
    @RequiredArgsConstructor
    private enum Validator {
        IS_LONG(s -> {
            try {
                Long.parseLong(s);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        private final Predicate<String> validator;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Parser {
        TO_LONG(Long::parseLong);
        private final Function<String, Object> parser;
    }    
}
