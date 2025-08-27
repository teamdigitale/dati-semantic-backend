package it.gov.innovazione.ndc.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class JenaLoadTest {
    private static final String DCAT_AP_IT = "http://dati.gov.it/onto/dcatapit#";
    public static final String CITIES_TTL_URL = "https://github.com/italia/dati-ontopia-virtuoso/raw/dev/vocabularies/cities.ttl";

    @Disabled("This connects to a remote URL and loads a lot of data in memory. Only run manually.")
    @Test
    void loadCities() {
        Model model = RDFDataMgr.loadModel(CITIES_TTL_URL);
        assertThat(model.isEmpty()).isFalse();

        Resource dataset = model.getResource(DCAT_AP_IT + "Dataset");
        assertThat(dataset).isNotNull();

        Statement statement = searchForDatasetStatement(model, dataset);
        Resource vocabulary = statement.getSubject();
        assertThat(vocabulary.getURI()).contains("cities");
    }

    private Statement searchForDatasetStatement(Model model, Resource dataset) {
        StmtIterator i = model.listStatements(null, RDF.type, dataset);
        try {
            return requireSingle(i);
        } finally {
            i.close();
        }
    }

    private <T> T requireSingle(Iterator<T> i) {
        assertThat(i.hasNext()).isTrue();
        T item = i.next();
        assertThat(i.hasNext()).isFalse();
        return item;
    }
}
