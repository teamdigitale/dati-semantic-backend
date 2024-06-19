package it.gov.innovazione.ndc.alerter.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.alerter.entities.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventService extends EntityService<Event> {

    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() {
    };
    @Getter
    private final EventRepository repository;
    private final ObjectMapper objectMapper;

    public void assertContextIsValid(String context) {
        try {
            objectMapper.readValue(context, TYPE_REF);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid context: " + e.getMessage());
        }
    }

}
