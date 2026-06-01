package it.gov.innovazione.ndc.repository;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.model.audit.ChangeKind;
import it.gov.innovazione.ndc.model.audit.ResourceDelta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourceDeltaRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String COLUMNS =
            "ID, HARVESTER_RUN_ID, ASSET_IRI, ASSET_TYPE, CHANGE_KIND, SUMMARY_JSON, CREATED_AT";

    private static final RowMapper<ResourceDelta> ROW_MAPPER = (rs, rowNum) -> ResourceDelta.builder()
            .id(rs.getString("ID"))
            .harvesterRunId(rs.getString("HARVESTER_RUN_ID"))
            .assetIri(rs.getString("ASSET_IRI"))
            .assetType(parseAssetType(rs))
            .changeKind(parseChangeKind(rs))
            .summaryJson(rs.getString("SUMMARY_JSON"))
            .createdAt(toInstant(rs, "CREATED_AT"))
            .build();

    public int save(ResourceDelta delta) {
        String sql = "INSERT INTO HARVEST_RESOURCE_DELTA ("
                + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                delta.getId(),
                delta.getHarvesterRunId(),
                delta.getAssetIri(),
                delta.getAssetType().name(),
                delta.getChangeKind().name(),
                delta.getSummaryJson(),
                Timestamp.from(delta.getCreatedAt()));
    }

    public void saveAll(List<ResourceDelta> deltas) {
        deltas.forEach(this::save);
    }

    public DeltaQueryResult findByRun(String runId, DeltaFilters filters, int offset, int limit) {
        StringBuilder where = new StringBuilder("WHERE HARVESTER_RUN_ID = ?");
        List<Object> params = new ArrayList<>();
        params.add(runId);
        appendFilters(where, params, filters);

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM HARVEST_RESOURCE_DELTA " + where,
                Integer.class, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(limit);
        pageParams.add(offset);
        List<ResourceDelta> content = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM HARVEST_RESOURCE_DELTA " + where
                        + " ORDER BY ASSET_IRI ASC LIMIT ? OFFSET ?",
                ROW_MAPPER, pageParams.toArray());
        return new DeltaQueryResult(content, total == null ? 0 : total);
    }

    private void appendFilters(StringBuilder where, List<Object> params, DeltaFilters filters) {
        if (filters == null) {
            return;
        }
        if (filters.getChangeKinds() != null && !filters.getChangeKinds().isEmpty()) {
            where.append(" AND CHANGE_KIND IN (")
                    .append(placeholders(filters.getChangeKinds().size())).append(")");
            filters.getChangeKinds().forEach(ck -> params.add(ck.name()));
        }
        if (filters.getAssetTypes() != null && !filters.getAssetTypes().isEmpty()) {
            where.append(" AND ASSET_TYPE IN (")
                    .append(placeholders(filters.getAssetTypes().size())).append(")");
            filters.getAssetTypes().forEach(at -> params.add(at.name()));
        }
        if (filters.getAssetIri() != null && !filters.getAssetIri().isBlank()) {
            where.append(" AND ASSET_IRI = ?");
            params.add(filters.getAssetIri());
        }
    }

    private static String placeholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    private static SemanticAssetType parseAssetType(ResultSet rs) throws SQLException {
        String raw = rs.getString("ASSET_TYPE");
        if (raw == null) {
            return null;
        }
        try {
            return SemanticAssetType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ChangeKind parseChangeKind(ResultSet rs) throws SQLException {
        String raw = rs.getString("CHANGE_KIND");
        if (raw == null) {
            return null;
        }
        try {
            return ChangeKind.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant toInstant(ResultSet rs, String column) {
        try {
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : ts.toInstant();
        } catch (SQLException e) {
            return null;
        }
    }

    public ChangelogResult findChangelog(String assetIri, DeltaFilters filters,
                                          Instant since, Instant until,
                                          int offset, int limit) {
        StringBuilder where = new StringBuilder(
                "WHERE d.ASSET_IRI = ?");
        List<Object> params = new ArrayList<>();
        params.add(assetIri);
        if (filters != null && filters.getChangeKinds() != null && !filters.getChangeKinds().isEmpty()) {
            where.append(" AND d.CHANGE_KIND IN (")
                    .append(placeholders(filters.getChangeKinds().size())).append(")");
            filters.getChangeKinds().forEach(ck -> params.add(ck.name()));
        }
        if (since != null) {
            where.append(" AND d.CREATED_AT >= ?");
            params.add(Timestamp.from(since));
        }
        if (until != null) {
            where.append(" AND d.CREATED_AT <= ?");
            params.add(Timestamp.from(until));
        }

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM HARVEST_RESOURCE_DELTA d " + where,
                Integer.class, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(limit);
        pageParams.add(offset);
        String sql = "SELECT d.ID, d.HARVESTER_RUN_ID, d.ASSET_IRI, d.ASSET_TYPE, d.CHANGE_KIND, "
                + "d.SUMMARY_JSON, d.CREATED_AT, "
                + "r.REPOSITORY_ID AS R_REPO_ID, r.REVISION AS R_REVISION, "
                + "r.REVISION_COMMITTED_AT AS R_COMMITTED_AT "
                + "FROM HARVEST_RESOURCE_DELTA d "
                + "LEFT JOIN HARVESTER_RUN r ON r.ID = d.HARVESTER_RUN_ID "
                + where + " ORDER BY d.CREATED_AT DESC LIMIT ? OFFSET ?";

        List<ChangelogRow> rows = jdbcTemplate.query(sql, (rs, n) -> new ChangelogRow(
                ResourceDelta.builder()
                        .id(rs.getString("ID"))
                        .harvesterRunId(rs.getString("HARVESTER_RUN_ID"))
                        .assetIri(rs.getString("ASSET_IRI"))
                        .assetType(parseAssetType(rs))
                        .changeKind(parseChangeKind(rs))
                        .summaryJson(rs.getString("SUMMARY_JSON"))
                        .createdAt(toInstant(rs, "CREATED_AT"))
                        .build(),
                rs.getString("R_REPO_ID"),
                rs.getString("R_REVISION"),
                toInstant(rs, "R_COMMITTED_AT")
        ), pageParams.toArray());

        return new ChangelogResult(rows, total == null ? 0 : total);
    }

    public Optional<DeltaSummaryCounters> summarizeByRun(String runId) {
        String sql = "SELECT CHANGE_KIND, ASSET_TYPE, COUNT(*) AS CNT "
                + "FROM HARVEST_RESOURCE_DELTA WHERE HARVESTER_RUN_ID = ? "
                + "GROUP BY CHANGE_KIND, ASSET_TYPE";
        DeltaSummaryCounters counters = new DeltaSummaryCounters();
        jdbcTemplate.query(sql, rs -> {
            String ck = rs.getString("CHANGE_KIND");
            String at = rs.getString("ASSET_TYPE");
            int cnt = rs.getInt("CNT");
            counters.add(at, ck, cnt);
        }, runId);
        return counters.isEmpty() ? Optional.empty() : Optional.of(counters);
    }

    public record DeltaQueryResult(List<ResourceDelta> content, int total) { }

    public record ChangelogRow(ResourceDelta delta, String repositoryId, String revision, Instant revisionCommittedAt) { }

    public record ChangelogResult(List<ChangelogRow> content, int total) { }

    @lombok.Data
    @lombok.Builder
    public static class DeltaFilters {
        private final List<ChangeKind> changeKinds;
        private final List<SemanticAssetType> assetTypes;
        private final String assetIri;
    }

    public static class DeltaSummaryCounters {
        private final java.util.Map<String, java.util.Map<String, Integer>> byAssetTypeAndKind = new java.util.LinkedHashMap<>();

        public void add(String assetType, String changeKind, int count) {
            byAssetTypeAndKind
                    .computeIfAbsent(assetType, k -> new java.util.LinkedHashMap<>())
                    .merge(changeKind, count, Integer::sum);
        }

        public boolean isEmpty() {
            return byAssetTypeAndKind.isEmpty();
        }

        public java.util.Map<String, java.util.Map<String, Integer>> getByAssetTypeAndKind() {
            return byAssetTypeAndKind;
        }
    }
}
