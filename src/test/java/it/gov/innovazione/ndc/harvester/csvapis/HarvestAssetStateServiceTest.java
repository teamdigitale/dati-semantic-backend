package it.gov.innovazione.ndc.harvester.csvapis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HarvestAssetStateServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    HarvestAssetStateService service;

    @BeforeEach
    void setup() {
        service = new HarvestAssetStateService(jdbcTemplate);
    }

    @Test
    void recordsDbDetectedRowWithExpectedColumns() {
        Instant detectedAt = Instant.parse("2026-05-05T10:00:00Z");

        service.recordDbDetected("run-id", "https://example/repo", "agencyId", "keyConcept",
                "abc123", detectedAt);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(contains("INSERT INTO HARVEST_ASSET_STATE"), args.capture());

        Object[] params = args.getAllValues().toArray();
        // Spring JdbcTemplate.update varargs is captured as a single Object[]; flatten:
        Object[] actual = (Object[]) params[0];
        if (actual.length == 1 && actual[0] instanceof Object[]) {
            actual = (Object[]) actual[0];
        }
        assertThat(actual).hasSize(7);
        // [0] random UUID
        assertThat(actual[0]).asString().isNotBlank();
        assertThat(actual[1]).isEqualTo("run-id");
        assertThat(actual[2]).isEqualTo("https://example/repo");
        assertThat(actual[3]).isEqualTo("agencyId");
        assertThat(actual[4]).isEqualTo("keyConcept");
        assertThat(actual[5]).isEqualTo("abc123");
        assertThat(actual[6]).isEqualTo(Timestamp.from(detectedAt));
    }
}
