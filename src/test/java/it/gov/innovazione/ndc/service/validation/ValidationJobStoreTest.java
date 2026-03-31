package it.gov.innovazione.ndc.service.validation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationJobStoreTest {

    @Test
    void shouldStoreAndRetrieveJob() {
        ValidationJobStore store = new ValidationJobStore();

        ValidationJob job = createJob("job-1");
        store.put(job);

        Optional<ValidationJob> found = store.find("job-1");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("job-1");
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
        ValidationJobStore store = new ValidationJobStore();

        assertThat(store.find("nonexistent")).isEmpty();
    }

    @Test
    void shouldUpdateLastAccessedAtOnFind() {
        ValidationJobStore store = new ValidationJobStore();

        ValidationJob job = createJob("job-1");
        store.put(job);
        Instant firstAccess = job.getLastAccessedAt();

        store.find("job-1");
        assertThat(job.getLastAccessedAt()).isAfterOrEqualTo(firstAccess);
    }

    @Test
    void shouldCleanExpiredJobs() {
        ValidationJobStore store = new ValidationJobStore();

        ValidationJob oldJob = createJob("old-job");
        oldJob.setLastAccessedAt(Instant.now().minusSeconds(3600));
        store.put(oldJob);
        oldJob.setLastAccessedAt(Instant.now().minusSeconds(3600));

        ValidationJob freshJob = createJob("fresh-job");
        store.put(freshJob);

        store.cleanExpiredJobs();

        assertThat(store.find("old-job")).isEmpty();
        assertThat(store.find("fresh-job")).isPresent();
    }

    @Test
    void shouldTrackSize() {
        ValidationJobStore store = new ValidationJobStore();

        assertThat(store.size()).isZero();

        store.put(createJob("job-1"));
        store.put(createJob("job-2"));

        assertThat(store.size()).isEqualTo(2);
    }

    private static ValidationJob createJob(String id) {
        return ValidationJob.builder()
                .id(id)
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.PENDING)
                .build();
    }
}
