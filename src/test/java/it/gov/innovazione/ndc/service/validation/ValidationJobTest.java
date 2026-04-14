package it.gov.innovazione.ndc.service.validation;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationJobTest {

    @Test
    void shouldReturnNullProgressWhenTotalAssetsNotSet() {
        ValidationJob job = ValidationJob.builder()
                .id("123")
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.PENDING)
                .build();

        assertThat(job.getProgress()).isNull();
    }

    @Test
    void shouldReturnProgressWhenTotalAssetsSet() {
        ValidationJob job = ValidationJob.builder()
                .id("123")
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.VALIDATING)
                .build();

        job.setTotalAssets(10);
        job.getProcessedAssets().set(5);

        ValidationJob.Progress progress = job.getProgress();

        assertThat(progress).isNotNull();
        assertThat(progress.processedAssets()).isEqualTo(5);
        assertThat(progress.totalAssets()).isEqualTo(10);
        assertThat(progress.percentage()).isEqualTo(50);
    }

    @Test
    void shouldReturnZeroPercentageWhenTotalAssetsIsZero() {
        ValidationJob job = ValidationJob.builder()
                .id("123")
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.VALIDATING)
                .build();

        job.setTotalAssets(0);

        ValidationJob.Progress progress = job.getProgress();

        assertThat(progress).isNotNull();
        assertThat(progress.percentage()).isZero();
    }

    @Test
    void shouldConstructRepoUrl() {
        ValidationJob job = ValidationJob.builder()
                .id("123")
                .owner("istat")
                .repo("ts-ontologie-vocabolari-controllati")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.PENDING)
                .build();

        assertThat(job.getRepoUrl()).isEqualTo("https://github.com/istat/ts-ontologie-vocabolari-controllati");
    }

    @Test
    void shouldTrackStatusChanges() {
        ValidationJob job = ValidationJob.builder()
                .id("123")
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.PENDING)
                .build();

        assertThat(job.getStatus()).isEqualTo(ValidationJob.Status.PENDING);

        job.setStatus(ValidationJob.Status.CLONING);
        assertThat(job.getStatus()).isEqualTo(ValidationJob.Status.CLONING);

        job.setStatus(ValidationJob.Status.COMPLETED);
        assertThat(job.getStatus()).isEqualTo(ValidationJob.Status.COMPLETED);
    }

    @Test
    void shouldStoreErrorMessage() {
        ValidationJob job = ValidationJob.builder()
                .id("123")
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.FAILED)
                .build();

        job.setErrorMessage("Clone failed");
        assertThat(job.getErrorMessage()).isEqualTo("Clone failed");
    }
}
