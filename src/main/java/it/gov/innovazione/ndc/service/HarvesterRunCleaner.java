package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
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
public class HarvesterRunCleaner {

    private final HarvesterRunService harvesterRunService;
    @Value("${harvester.run-cleaner.retention:P120D}")
    private Period retention;

    // schedule each day at 01:00
    @Scheduled(cron = "${harvester.run-cleaner.cron:0 0 1 * * *}")
    void cleanOldRuns() {
        Instant threshold = Instant.now().minus(retention);
        log.info("Cleaning harvester runs older than {} days", retention.getDays());
        long count = harvesterRunService.deleteRunsOlderThan(threshold);
        log.info("Deleted {} harvester runs older than {}", count, threshold);
    }
}