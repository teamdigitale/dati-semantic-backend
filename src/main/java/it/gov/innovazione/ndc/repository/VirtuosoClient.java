package it.gov.innovazione.ndc.repository;

import org.apache.jena.http.auth.AuthEnv;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Objects;

@Component
public class VirtuosoClient {
    private final TripleStoreProperties properties;
    private final HttpClient httpClient;

    public VirtuosoClient(TripleStoreProperties properties) {
        this.properties = properties;
        this.httpClient = createHttpClient();
    }

    public RDFConnection getConnection() {
        RDFConnection rdfConnection = RDFConnectionRemote.create()
            .queryEndpoint(properties.getSparql())
            .updateEndpoint(properties.getSparql())
            .gspEndpoint(properties.getSparqlGraphStore())
            .httpClient(this.httpClient)
            .build();
        return Objects.requireNonNull(rdfConnection);
    }

    public String getSparqlEndpoint() {
        return properties.getSparql();
    }

    private HttpClient createHttpClient() {
        AuthEnv.get().registerUsernamePassword(URI.create(properties.getSparql()), properties.getUsername(), properties.getPassword());
        AuthEnv.get().registerUsernamePassword(URI.create(properties.getSparqlGraphStore()), properties.getUsername(), properties.getPassword());
        return HttpClient.newBuilder().build();
    }
}
