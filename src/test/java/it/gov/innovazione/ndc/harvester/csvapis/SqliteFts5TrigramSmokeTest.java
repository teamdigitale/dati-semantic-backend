package it.gov.innovazione.ndc.harvester.csvapis;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the bundled sqlite-jdbc driver supports the FTS5 virtual table
 * with the {@code trigram} tokenizer. Required by the DEV-CSV merge: upstream's
 * APIStore.create_fts_table builds {@code _metadata_fts} with that exact tokenizer,
 * and the aggregated DB will not be usable by apiv1 if the same tokenizer is not
 * available at write time.
 */
class SqliteFts5TrigramSmokeTest {

    @Test
    void fts5VirtualTableWithTrigramTokenizerWorks() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement st = conn.createStatement()) {

            st.execute("CREATE VIRTUAL TABLE smoke_fts "
                    + "USING fts5(title, description, catalog, tokenize = 'trigram')");

            st.execute("INSERT INTO smoke_fts(title, description, catalog) "
                    + "VALUES ('Vocabolario nazionale', 'Interoperabilita semantica', 'catalog-a')");
            st.execute("INSERT INTO smoke_fts(title, description, catalog) "
                    + "VALUES ('Catalogo dati', 'Pubblicazione dei vocabolari controllati', 'catalog-b')");

            List<String> matches = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT title FROM smoke_fts WHERE smoke_fts MATCH 'cabolario'")) {
                while (rs.next()) {
                    matches.add(rs.getString(1));
                }
            }

            assertThat(matches)
                    .as("trigram tokenizer must allow substring matches")
                    .containsExactly("Vocabolario nazionale");
        }
    }
}
