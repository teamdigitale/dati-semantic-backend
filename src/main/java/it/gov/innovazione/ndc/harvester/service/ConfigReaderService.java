package it.gov.innovazione.ndc.harvester.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConfigReaderService {

    private static final TypeReference<Map<ActualConfigService.ConfigKey, ConfigService.ConfigEntry>> TYPE_REF = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public String fromMap(Map<ActualConfigService.ConfigKey, ConfigService.ConfigEntry> config) {
        return objectMapper.writeValueAsString(config);
    }

    public Map<ActualConfigService.ConfigKey, ConfigService.ConfigEntry> toMap(String value) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, TYPE_REF);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
