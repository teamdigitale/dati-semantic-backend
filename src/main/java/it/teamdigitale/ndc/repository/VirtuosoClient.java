package it.teamdigitale.ndc.repository;

import java.util.Objects;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.springframework.stereotype.Component;

@Component
public class VirtuosoClient {
    private final TripleStoreProperties properties;
    private final HttpClient httpClient;

    public VirtuosoClient(TripleStoreProperties properties) {
        this.properties = properties;
        this.httpClient = createHttpClient(properties.getUsername(), properties.getPassword());
    }

    public HttpClient getHttpClient() {
        return httpClient;
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

    private HttpClient createHttpClient(String username, String password) {
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        return HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    }
}
