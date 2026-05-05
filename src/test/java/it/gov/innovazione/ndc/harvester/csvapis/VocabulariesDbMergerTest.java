package it.gov.innovazione.ndc.harvester.csvapis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test that builds two minimal APIStore-shaped SQLite databases,
 * runs the merger, and verifies the resulting aggregate against the contract
 * the upstream {@code apiv1} expects.
 */
class VocabulariesDbMergerTest {

    private final VocabulariesDbMerger merger = new VocabulariesDbMerger();

    @Test
    void mergesTwoVocabulariesAndBuildsFtsIndex(@TempDir Path tempDir) throws Exception {
        Path sourceA = tempDir.resolve("a.db");
        Path sourceB = tempDir.resolve("b.db");
        Path output = tempDir.resolve("vocabularies.db");

        createApiStoreSourceDb(sourceA,
                "https://w3id.org/italia/cv/agencyA/cities",
                "agencyA", "cities",
                "{\"info\":{\"title\":\"Citta italiane\","
                        + "\"description\":\"Codelist dei comuni\"}}",
                "{\"keywords\":[\"comuni\"]}",
                "agencyA_cities",
                List.of(new DataRow("rome", "Roma"), new DataRow("milan", "Milano")));

        createApiStoreSourceDb(sourceB,
                "https://w3id.org/italia/cv/agencyB/professions",
                "agencyB", "professions",
                "{\"info\":{\"title\":\"Professioni\","
                        + "\"description\":\"Tassonomia delle professioni regolamentate\"}}",
                "{\"keywords\":[\"professioni\"]}",
                "agencyB_professions",
                List.of(new DataRow("doctor", "Medico"), new DataRow("teacher", "Insegnante")));

        merger.merge(List.of(sourceA, sourceB), output);

        try (Connection conn = openOutput(output)) {
            assertIntegrityOk(conn);
            assertMetadataRows(conn, 2);
            assertHasTable(conn, "agencyA_cities");
            assertHasTable(conn, "agencyB_professions");
            assertDataRowCount(conn, "agencyA_cities", 2);
            assertDataRowCount(conn, "agencyB_professions", 2);
            assertFtsCount(conn, 2);
            assertFtsTrigramSearch(conn, "ofessio", "Professioni");
            assertFtsTrigramSearch(conn, "munei", null);
            assertFtsTrigramSearch(conn, "comuni", "Citta italiane");
        }
    }

    @Test
    void rerunningWithUpdatedSourceUpsertsTheRow(@TempDir Path tempDir) throws Exception {
        Path source = tempDir.resolve("a.db");
        Path output = tempDir.resolve("vocabularies.db");

        createApiStoreSourceDb(source,
                "https://w3id.org/italia/cv/agencyA/cities",
                "agencyA", "cities",
                "{\"info\":{\"title\":\"Citta v1\",\"description\":\"\"}}",
                "{}",
                "agencyA_cities",
                List.of(new DataRow("rome", "Roma")));

        merger.merge(List.of(source), output);

        // Update v1 -> v2 with same agency+concept (must upsert, not duplicate)
        createApiStoreSourceDb(source,
                "https://w3id.org/italia/cv/agencyA/cities",
                "agencyA", "cities",
                "{\"info\":{\"title\":\"Citta v2\",\"description\":\"\"}}",
                "{}",
                "agencyA_cities",
                List.of(new DataRow("rome", "Roma"), new DataRow("milan", "Milano")));

        merger.merge(List.of(source), output);

        try (Connection conn = openOutput(output)) {
            assertMetadataRows(conn, 1);
            String title = queryString(conn,
                    "SELECT json_extract(openapi, '$.info.title') FROM _metadata");
            assertThat(title).isEqualTo("Citta v2");
            assertDataRowCount(conn, "agencyA_cities", 2);
            assertFtsCount(conn, 1);
        }
    }

    @Test
    void failsWhenSourceIsMissingMetadataTable(@TempDir Path tempDir) throws Exception {
        Path source = tempDir.resolve("bad.db");
        Path output = tempDir.resolve("out.db");
        try (Connection conn = openSource(source);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE not_metadata (id INTEGER)");
        }

        assertThatThrownBy(() -> merger.merge(List.of(source), output))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("_metadata");
    }

    @Test
    void rejectsEmptySourceList(@TempDir Path tempDir) {
        Path output = tempDir.resolve("out.db");

        assertThatThrownBy(() -> merger.merge(List.of(), output))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private record DataRow(String id, String label) {
    }

    private void createApiStoreSourceDb(Path file,
                                        String vocabularyUri,
                                        String agencyId,
                                        String keyConcept,
                                        String openapiJson,
                                        String catalogJson,
                                        String dataTable,
                                        List<DataRow> rows) throws IOException, SQLException {
        java.nio.file.Files.deleteIfExists(file);
        try (Connection conn = openSource(file)) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE _metadata ("
                        + "vocabulary_uri TEXT NOT NULL, "
                        + "agency_id TEXT NOT NULL, "
                        + "key_concept TEXT NOT NULL, "
                        + "openapi TEXT NOT NULL, "
                        + "catalog TEXT NOT NULL)");
                st.execute("CREATE UNIQUE INDEX agency_id_key_concept_unique "
                        + "ON _metadata(agency_id, key_concept)");
                st.execute("CREATE TABLE \"" + dataTable + "\" ("
                        + "id TEXT PRIMARY KEY, label TEXT NOT NULL)");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO _metadata(vocabulary_uri, agency_id, key_concept, openapi, catalog) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, vocabularyUri);
                ps.setString(2, agencyId);
                ps.setString(3, keyConcept);
                ps.setString(4, openapiJson);
                ps.setString(5, catalogJson);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO \"" + dataTable + "\"(id, label) VALUES (?, ?)")) {
                for (DataRow row : rows) {
                    ps.setString(1, row.id());
                    ps.setString(2, row.label());
                    ps.executeUpdate();
                }
            }
        }
    }

    private Connection openSource(Path file) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
    }

    private Connection openOutput(Path file) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
    }

    private void assertIntegrityOk(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("ok");
        }
    }

    private void assertMetadataRows(Connection conn, int expected) throws SQLException {
        assertThat(queryInt(conn, "SELECT COUNT(*) FROM _metadata")).isEqualTo(expected);
    }

    private void assertHasTable(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next())
                        .as("table %s must exist in aggregate", name)
                        .isTrue();
            }
        }
    }

    private void assertDataRowCount(Connection conn, String table, int expected) throws SQLException {
        assertThat(queryInt(conn, "SELECT COUNT(*) FROM \"" + table + "\""))
                .isEqualTo(expected);
    }

    private void assertFtsCount(Connection conn, int expected) throws SQLException {
        assertThat(queryInt(conn, "SELECT COUNT(*) FROM _metadata_fts"))
                .isEqualTo(expected);
    }

    private void assertFtsTrigramSearch(Connection conn, String query, String expectedTitle) throws SQLException {
        List<String> titles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT title FROM _metadata_fts WHERE _metadata_fts MATCH ?")) {
            ps.setString(1, query);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    titles.add(rs.getString(1));
                }
            }
        }
        if (expectedTitle == null) {
            assertThat(titles).as("trigram search '%s' should not match", query).isEmpty();
        } else {
            assertThat(titles).as("trigram search '%s'", query).containsExactly(expectedTitle);
        }
    }

    private int queryInt(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            return rs.getInt(1);
        }
    }

    private String queryString(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            return rs.getString(1);
        }
    }
}
