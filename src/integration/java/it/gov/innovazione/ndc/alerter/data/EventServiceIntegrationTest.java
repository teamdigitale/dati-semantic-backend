package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Event;
import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs without a test-managed transaction, like the scheduled cleaner does.
 */
@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import(EventService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EventServiceIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @MockitoBean
    private NdcEventPublisher eventPublisher;

    @MockitoBean
    private EventMapper eventMapper;

    @AfterEach
    void cleanUp() {
        eventRepository.deleteAll();
    }

    @Test
    void shouldDeleteOnlyEventsOlderThanThresholdAndReturnTheirCount() {
        Instant now = Instant.now();
        Instant threshold = now.minus(Duration.ofDays(180));
        eventRepository.saveAll(List.of(
                event("old-1", threshold.minus(Duration.ofDays(10))),
                event("old-2", threshold.minus(Duration.ofDays(1))),
                event("old-3", threshold.minusSeconds(1)),
                event("recent-1", threshold.plusSeconds(1)),
                event("recent-2", now)));

        long deleted = eventService.deleteOlderThanAndGetCount(threshold);

        assertThat(deleted).isEqualTo(3);
        assertThat(eventRepository.findAll())
                .extracting(Event::getName)
                .containsExactlyInAnyOrder("recent-1", "recent-2");
    }

    @Test
    void shouldReturnZeroWhenNothingToDelete() {
        Instant now = Instant.now();
        eventRepository.save(event("recent", now));

        long deleted = eventService.deleteOlderThanAndGetCount(now.minus(Duration.ofDays(180)));

        assertThat(deleted).isZero();
        assertThat(eventRepository.count()).isEqualTo(1);
    }

    private static Event event(String name, Instant createdAt) {
        Event event = new Event();
        event.setName(name);
        event.setDescription("description");
        event.setCategory(EventCategory.APPLICATION);
        event.setSeverity(Severity.INFO);
        event.setContext("{}");
        event.setOccurredAt(createdAt);
        event.setCreatedAt(createdAt);
        event.setCreatedBy("test");
        return event;
    }
}
