package it.gov.innovazione.ndc.harvester.csvapis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class HarvestAssetStateService {

    private static final String INSERT_SQL =
            "INSERT INTO HARVEST_ASSET_STATE "
                    + "(ID, HARVESTER_RUN_ID, REPO_URL, AGENCY_ID, KEY_CONCEPT, SOURCE_DB_HASH, DETECTED_AT) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final Path workDir;

    public HarvestAssetStateService(
            JdbcTemplate jdbcTemplate,
            @Value("${harvester.csvapis.aggregate-db.work-dir}") String workDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.workDir = Path.of(workDir);
    }

    public void recordDbDetected(String harvesterRunId,
                                 String repoUrl,
                                 String agencyId,
                                 String keyConcept,
                                 String sourceDbHash,
                                 Path sourceDbFile,
                                 Instant detectedAt) {
        copyToWorkDir(agencyId, keyConcept, sourceDbFile);
        jdbcTemplate.update(INSERT_SQL,
                UUID.randomUUID().toString(),
                harvesterRunId,
                repoUrl,
                agencyId,
                keyConcept,
                sourceDbHash,
                Timestamp.from(detectedAt));
    }

    public Path workDirEntryFor(String agencyId, String keyConcept) {
        return workDir.resolve(sanitize(agencyId)).resolve(sanitize(keyConcept) + ".db");
    }

    private void copyToWorkDir(String agencyId, String keyConcept, Path source) {
        Path target = workDirEntryFor(agencyId, keyConcept);
        try {
            Files.createDirectories(Objects.requireNonNull(target.getParent()));
            Files.copy(source, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            log.info("Copied APIStore .db to work-dir: {}", target);
        } catch (IOException e) {
            // Non-fatal: row is still recorded so the asset shows up in the report;
            // the aggregation step will skip rows whose work-dir entry is missing.
            log.error("Failed to copy APIStore .db to work-dir for {}/{} (target={})",
                    agencyId, keyConcept, target, e);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
