package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.entities.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class EventService extends EntityService<Event, EventDto> {

    @Getter(AccessLevel.PROTECTED)
    private final EventRepository repository;

    @Getter(AccessLevel.PROTECTED)
    private final EventMapper entityMapper;

    @Getter(AccessLevel.PROTECTED)
    private final String entityName = "Event";

    @Getter(AccessLevel.PROTECTED)
    private final Sort defaultSorting = Sort.by("occurredAt").descending();

    @Override
    protected void assertEntityDoesNotExists(EventDto dto) {
        if (repository.existsByNameAndOccurredAt(dto.getName(), dto.getOccurredAt())) {
            throw new ConflictingOperationException("An event with the same name/occurredAt already exists: " + dto.getName() + "/" + dto.getOccurredAt());
        }
    }

    public List<EventDto> getEventsNewerThan(Instant instant) {
        return repository.findByCreatedAtAfter(instant).stream()
                .map(entityMapper::toDto)
                .collect(toList());
    }
}
