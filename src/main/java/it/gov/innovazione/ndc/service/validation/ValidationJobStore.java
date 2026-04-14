package it.gov.innovazione.ndc.service.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ValidationJobStore {

    private static final Duration TTL = Duration.ofMinutes(30);

    private final ConcurrentHashMap<String, ValidationJob> jobs = new ConcurrentHashMap<>();

    public void put(ValidationJob job) {
        job.setLastAccessedAt(Instant.now());
        jobs.put(job.getId(), job);
    }

    public Optional<ValidationJob> find(String id) {
        ValidationJob job = jobs.get(id);
        if (job != null) {
            job.setLastAccessedAt(Instant.now());
        }
        return Optional.ofNullable(job);
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanExpiredJobs() {
        Instant cutoff = Instant.now().minus(TTL);
        int before = jobs.size();
        jobs.entrySet().removeIf(entry -> {
            Instant lastAccess = entry.getValue().getLastAccessedAt();
            return lastAccess != null && lastAccess.isBefore(cutoff);
        });
        int removed = before - jobs.size();
        if (removed > 0) {
            log.info("Cleaned {} expired validation jobs", removed);
        }
    }

    public int size() {
        return jobs.size();
    }
}
