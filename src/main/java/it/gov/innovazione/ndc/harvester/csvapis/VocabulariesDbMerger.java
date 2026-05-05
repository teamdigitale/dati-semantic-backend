package it.gov.innovazione.ndc.harvester.csvapis;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges N per-asset SQLite databases (produced by {@code apistore create}) into
 * a single aggregate database, preserving the APIStore schema expected by
 * {@code dati-semantic-csv-apis}/{@code apiv1}.
 *
 * <p>Strategy: ATTACH each source, copy the {@code _metadata} table DDL once,
 * UPSERT metadata rows by unique key, copy each source's per-vocabulary data
 * tables, then build the {@code _metadata_fts} FTS5 trigram index over the
 * aggregated metadata.
 */
@Slf4j
public class VocabulariesDbMerger {

    private static final String METADATA_TABLE = "_metadata";
    private static final String METADATA_FTS_TABLE = "_metadata_fts";

    public void merge(List<Path> sources, Path output) throws IOException, SQLException {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("At least one source database is required");
        }
        Files.deleteIfExists(output);

        String jdbcUrl = "jdbc:sqlite:" + output.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            // Auto-commit must stay on so that ATTACH/DETACH DATABASE succeed:
            // SQLite forbids ATTACH inside an explicit transaction. Atomicity
            // of the aggregate file is provided by the upstream swap step (F5).
            boolean metadataSchemaCopied = false;
            for (Path source : sources) {
                attachSource(conn, source);
                try {
                    if (!metadataSchemaCopied) {
                        copyMetadataSchemaFromSource(conn);
                        metadataSchemaCopied = true;
                    }
                    upsertMetadataRows(conn);
                    copyDataTablesFromSource(conn);
                } finally {
                    detachSource(conn);
                }
            }
            buildFtsIndex(conn);
            validateAggregate(conn);
            log.info("Aggregated {} source database(s) into {}", sources.size(), output);
        }
    }

    private void attachSource(Connection conn, Path source) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("ATTACH DATABASE ? AS src")) {
            ps.setString(1, source.toAbsolutePath().toString());
            ps.execute();
        }
    }

    private void detachSource(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DETACH DATABASE src");
        }
    }

    private void copyMetadataSchemaFromSource(Connection conn) throws SQLException {
        String tableDdl = readDdl(conn, "table", METADATA_TABLE);
        if (tableDdl == null) {
            throw new IllegalStateException(
                    "Source database does not contain table " + METADATA_TABLE);
        }
        executeDdl(conn, tableDdl);
        for (String indexDdl : readIndexDdls(conn, METADATA_TABLE)) {
            executeDdl(conn, indexDdl);
        }
    }

    private void upsertMetadataRows(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT OR REPLACE INTO main." + quoteIdent(METADATA_TABLE)
                    + " SELECT * FROM src." + quoteIdent(METADATA_TABLE));
        }
    }

    private void copyDataTablesFromSource(Connection conn) throws SQLException {
        for (String table : listSourceDataTables(conn)) {
            copyDataTable(conn, table);
        }
    }

    private List<String> listSourceDataTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM src.sqlite_master "
                             + "WHERE type='table' "
                             + "  AND name <> '" + METADATA_TABLE + "' "
                             + "  AND name NOT LIKE 'sqlite_%' "
                             + "  AND name NOT LIKE '" + METADATA_FTS_TABLE + "%'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private void copyDataTable(Connection conn, String table) throws SQLException {
        String quoted = quoteIdent(table);
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS main." + quoted);
        }
        String ddl = readDdl(conn, "table", table);
        if (ddl != null) {
            executeDdl(conn, ddl);
        }
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO main." + quoted + " SELECT * FROM src." + quoted);
        }
        for (String indexDdl : readIndexDdls(conn, table)) {
            executeDdl(conn, indexDdl);
        }
    }

    private void validateAggregate(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
            if (!rs.next() || !"ok".equalsIgnoreCase(rs.getString(1))) {
                throw new IllegalStateException("Aggregate database failed PRAGMA integrity_check");
            }
        }
        int metadataRows;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + quoteIdent(METADATA_TABLE))) {
            rs.next();
            metadataRows = rs.getInt(1);
        }
        int ftsRows;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + quoteIdent(METADATA_FTS_TABLE))) {
            rs.next();
            ftsRows = rs.getInt(1);
        }
        if (metadataRows != ftsRows) {
            throw new IllegalStateException(
                    "FTS index is out of sync: " + ftsRows + " rows vs " + metadataRows
                            + " in " + METADATA_TABLE);
        }
        if (metadataRows > 0) {
            int dataTables;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) FROM main.sqlite_master "
                                 + "WHERE type='table' "
                                 + "  AND name <> '" + METADATA_TABLE + "' "
                                 + "  AND name NOT LIKE 'sqlite_%' "
                                 + "  AND name NOT LIKE '" + METADATA_FTS_TABLE + "%'")) {
                rs.next();
                dataTables = rs.getInt(1);
            }
            if (dataTables < metadataRows) {
                throw new IllegalStateException(
                        "Aggregate has " + metadataRows + " metadata rows but only "
                                + dataTables + " data table(s)");
            }
        }
        // Smoke FTS query: must not throw, regardless of result set size.
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT 1 FROM " + quoteIdent(METADATA_FTS_TABLE)
                             + " WHERE " + quoteIdent(METADATA_FTS_TABLE) + " MATCH 'a' LIMIT 1")) {
            rs.next();
        }
    }

    private void buildFtsIndex(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE VIRTUAL TABLE IF NOT EXISTS " + quoteIdent(METADATA_FTS_TABLE)
                    + " USING fts5(title, description, catalog, tokenize = 'trigram')");
            st.execute("DELETE FROM " + quoteIdent(METADATA_FTS_TABLE));
            st.execute(
                    "INSERT INTO " + quoteIdent(METADATA_FTS_TABLE)
                            + "(rowid, title, description, catalog) "
                            + "SELECT m.rowid, "
                            + "       json_extract(m.openapi, '$.info.title'), "
                            + "       json_extract(m.openapi, '$.info.description'), "
                            + "       m.catalog "
                            + "  FROM " + quoteIdent(METADATA_TABLE) + " m");
        }
    }

    private String readDdl(Connection conn, String type, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sql FROM src.sqlite_master WHERE type = ? AND name = ?")) {
            ps.setString(1, type);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private List<String> readIndexDdls(Connection conn, String table) throws SQLException {
        List<String> ddls = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sql FROM src.sqlite_master "
                        + "WHERE type = 'index' AND tbl_name = ? AND sql IS NOT NULL")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ddls.add(rs.getString(1));
                }
            }
        }
        return ddls;
    }

    private void executeDdl(Connection conn, String ddl) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }

    private static String quoteIdent(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
