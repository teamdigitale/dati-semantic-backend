package it.gov.innovazione.ndc.harvester.csvapis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * End-of-run aggregator that, given the latest {@code .db} per asset captured in
 * {@code HARVEST_ASSET_STATE} and copied to the work-dir by {@link HarvestAssetStateService},
 * produces the unified {@code vocabularies.db} via {@link VocabulariesDbMerger}.
 *
 * <p>Idempotent: if the sha256 of the sorted set of source hashes is unchanged
 * since the previous successful aggregation (recorded in a sidecar
 * {@code .aggregate-hash} file), the merge is skipped. Otherwise the new file
 * is built into a {@code .tmp} sibling and atomically renamed into place.
 */
@Service
@Slf4j
public class VocabulariesDbAggregationService {

    private static final String LATEST_PER_ASSET_SQL =
            "SELECT AGENCY_ID, KEY_CONCEPT, SOURCE_DB_HASH FROM ("
                    + "  SELECT AGENCY_ID, KEY_CONCEPT, SOURCE_DB_HASH,"
                    + "         ROW_NUMBER() OVER ("
                    + "           PARTITION BY AGENCY_ID, KEY_CONCEPT"
                    + "           ORDER BY DETECTED_AT DESC, ID DESC) AS rn"
                    + "    FROM HARVEST_ASSET_STATE) t "
                    + "WHERE rn = 1";

    private final JdbcTemplate jdbcTemplate;
    private final VocabulariesDbMerger merger;
    private final HarvestAssetStateService stateService;
    private final Path aggregatePath;

    public VocabulariesDbAggregationService(
            JdbcTemplate jdbcTemplate,
            VocabulariesDbMerger merger,
            HarvestAssetStateService stateService,
            @Value("${harvester.csvapis.aggregate-db.path}") String aggregatePath) {
        this.jdbcTemplate = jdbcTemplate;
        this.merger = merger;
        this.stateService = stateService;
        this.aggregatePath = Path.of(aggregatePath);
    }

    public void aggregateIfNeeded() {
        try {
            doAggregate();
        } catch (Exception e) {
            log.error("Aggregation step failed: {}", e.getMessage(), e);
        }
    }

    private void doAggregate() throws IOException, SQLException {
        List<AssetEntry> entries = loadLatestPerAsset();
        if (entries.isEmpty()) {
            log.info("No assets registered in HARVEST_ASSET_STATE; skipping aggregation");
            return;
        }

        List<Path> sourcePaths = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        for (AssetEntry entry : entries) {
            Path source = stateService.workDirEntryFor(entry.agencyId(), entry.keyConcept());
            if (!Files.isRegularFile(source)) {
                log.warn("Work-dir entry missing for {}/{} at {}; skipping",
                        entry.agencyId(), entry.keyConcept(), source);
                continue;
            }
            sourcePaths.add(source);
            hashes.add(entry.sourceDbHash());
        }
        if (sourcePaths.isEmpty()) {
            log.info("No work-dir entries available; skipping aggregation");
            return;
        }

        String currentHash = aggregateHashOf(hashes);
        Path hashFile = sidecarHashFile();
        if (Files.isRegularFile(aggregatePath) && Files.isRegularFile(hashFile)
                && currentHash.equals(Files.readString(hashFile).trim())) {
            log.info("Aggregate is up-to-date (hash={}); skipping merge", currentHash);
            return;
        }

        Path parent = aggregatePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmpAggregate = withSuffix(aggregatePath, ".tmp");
        Files.deleteIfExists(tmpAggregate);
        merger.merge(sourcePaths, tmpAggregate);

        Path tmpHashFile = withSuffix(hashFile, ".tmp");
        Files.writeString(tmpHashFile, currentHash);

        moveAtomic(tmpAggregate, aggregatePath);
        moveAtomic(tmpHashFile, hashFile);
        log.info("Published new aggregate to {} ({} sources, hash={})",
                aggregatePath, sourcePaths.size(), currentHash);
    }

    private Path sidecarHashFile() {
        return Path.of(aggregatePath.toString() + ".aggregate-hash");
    }

    private List<AssetEntry> loadLatestPerAsset() {
        return jdbcTemplate.query(LATEST_PER_ASSET_SQL, (rs, rowNum) -> new AssetEntry(
                rs.getString("AGENCY_ID"),
                rs.getString("KEY_CONCEPT"),
                rs.getString("SOURCE_DB_HASH")));
    }

    private static Path withSuffix(Path path, String suffix) {
        return Path.of(path.toString() + suffix);
    }

    private static void moveAtomic(Path src, Path dst) throws IOException {
        try {
            Files.move(src, dst,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            // Fallback for filesystems where atomic moves are not available
            // (e.g. across mount points). Still safer than a partial write.
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String aggregateHashOf(List<String> hashes) {
        List<String> sorted = new ArrayList<>(hashes);
        Collections.sort(sorted);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String h : sorted) {
                md.update(h.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }

    public record AssetEntry(String agencyId, String keyConcept, String sourceDbHash) {
    }
}
