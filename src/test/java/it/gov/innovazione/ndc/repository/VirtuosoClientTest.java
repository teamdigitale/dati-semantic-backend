package it.gov.innovazione.ndc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VirtuosoClientTest {

    @Mock
    private TripleStoreProperties properties;
    @Mock
    private RDFConnection connection;
    @Mock
    private RDFConnectionRemoteBuilder remoteBuilder;

    private VirtuosoClient virtuosoClient;


    @BeforeEach
    public void setUp() {
        when(properties.getUsername()).thenReturn("username");
        when(properties.getPassword()).thenReturn("password");

        virtuosoClient = new VirtuosoClient(properties);
    }


    @Test
    void shouldReturnHttpClient() {
        HttpClient client = virtuosoClient.getHttpClient();

        assertThat(client).isInstanceOf(CloseableHttpClient.class);
        assertThat(client).isNotNull();
    }

    @Test
    void shouldReturnSparqlEndpoint() {
        when(properties.getSparql()).thenReturn("http://localhost:8890/sparql");

        String sparqlEndpoint = virtuosoClient.getSparqlEndpoint();

        assertThat(sparqlEndpoint).isEqualTo("http://localhost:8890/sparql");
    }

    @Test
    void shouldGetConnection() {
        when(properties.getSparql()).thenReturn("http://localhost:8890/sparql");
        when(properties.getSparqlGraphStore()).thenReturn(
            "http://localhost:8890/sparql-graph-store");
        when(remoteBuilder.queryEndpoint("http://localhost:8890/sparql")).thenReturn(remoteBuilder);
        when(remoteBuilder.updateEndpoint("http://localhost:8890/sparql")).thenReturn(
            remoteBuilder);
        when(remoteBuilder.gspEndpoint("http://localhost:8890/sparql-graph-store"))
            .thenReturn(remoteBuilder);
        when(remoteBuilder.httpClient(any())).thenReturn(remoteBuilder);
        when(remoteBuilder.build()).thenReturn(connection);

        try (MockedStatic<RDFConnectionRemote> mockedStatic = getMockedStatic()) {
            RDFConnection connection = virtuosoClient.getConnection();
            assertThat(connection).isEqualTo(this.connection);

            mockedStatic.verify(RDFConnectionRemote::create, times(1));
        }

        verify(remoteBuilder).queryEndpoint("http://localhost:8890/sparql");
        verify(remoteBuilder).updateEndpoint("http://localhost:8890/sparql");
        verify(remoteBuilder).gspEndpoint("http://localhost:8890/sparql-graph-store");
        verify(remoteBuilder).httpClient(any());
        verify(remoteBuilder).build();
    }

    private MockedStatic<RDFConnectionRemote> getMockedStatic() {
        MockedStatic<RDFConnectionRemote> mockedStatic =
            Mockito.mockStatic(RDFConnectionRemote.class);
        mockedStatic.when(RDFConnectionRemote::create).thenReturn(remoteBuilder);
        return mockedStatic;
    }
}