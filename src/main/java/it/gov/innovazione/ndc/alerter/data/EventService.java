package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService extends EntityService<Event> {
    @Getter(AccessLevel.PROTECTED)
    private final EventRepository repository;
    @Getter(AccessLevel.PROTECTED)
    private final String entityName = "Event";

}
