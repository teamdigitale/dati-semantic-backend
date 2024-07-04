package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.alerter.data.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Period;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventCleaner {

    private final EventService eventService;
    @Value("${alerter.event-cleaner.retention:P180D}")
    private Period retention;

    // schedule each day at 00:00
    @Scheduled(cron = "0 0 0 * * *")
    void cleanEvents() {
        Instant threshold = Instant.now().minus(retention);
        log.info("Cleaning events older than {} days", retention.getDays());
        long count = eventService.deleteOlderThanAndGetCount(threshold);
        log.info("Deleted {} events older than {}", count, threshold);
    }
}
