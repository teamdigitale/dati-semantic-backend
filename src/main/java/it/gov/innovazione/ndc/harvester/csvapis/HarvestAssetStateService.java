package it.gov.innovazione.ndc.harvester.csvapis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HarvestAssetStateService {

    private static final String INSERT_SQL =
            "INSERT INTO HARVEST_ASSET_STATE "
                    + "(ID, HARVESTER_RUN_ID, REPO_URL, AGENCY_ID, KEY_CONCEPT, SOURCE_DB_HASH, DETECTED_AT) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public void recordDbDetected(String harvesterRunId,
                                 String repoUrl,
                                 String agencyId,
                                 String keyConcept,
                                 String sourceDbHash,
                                 Instant detectedAt) {
        jdbcTemplate.update(INSERT_SQL,
                UUID.randomUUID().toString(),
                harvesterRunId,
                repoUrl,
                agencyId,
                keyConcept,
                sourceDbHash,
                Timestamp.from(detectedAt));
    }
}
