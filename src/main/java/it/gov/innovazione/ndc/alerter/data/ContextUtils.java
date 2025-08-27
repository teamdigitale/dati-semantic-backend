package it.gov.innovazione.ndc.alerter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ContextUtils {

    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public String fromContext(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error while serializing context: " + context, e);
        }
    }

    public Map<String, Object> toContext(String context) {
        if (context == null || context.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(defaultContextIfNull(context), TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error while deserializing context: " + context, e);
        }
    }

    private String defaultContextIfNull(String context) {
        return Objects.nonNull(context) ? context : "{}";
    }

}
