package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Event;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
interface EventRepository extends NameableRepository<Event, String> {

    boolean existsByNameAndOccurredAt(String name, Instant occurredAt);

    List<Event> findByCreatedAtAfter(Instant instant);
}
