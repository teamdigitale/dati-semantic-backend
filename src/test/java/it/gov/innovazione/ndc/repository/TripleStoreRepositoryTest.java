package it.gov.innovazione.ndc.repository;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.http.client.HttpClient;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripleStoreRepositoryTest {
    private static final String REPO_URL = "http://www.repos.org/reponame";

    @Mock
    RDFConnection connection;
    @Mock
    UpdateProcessor updateProcessor;
    @Mock
    HttpClient httpClient;
    @Mock
    VirtuosoClient virtuosoClient;

    @InjectMocks
    TripleStoreRepository tripleStoreRepository;

    @Test
    void shouldConnectAndLoadModelWhenSaving() {
        Model model = createSimpleModel();
        when(virtuosoClient.getConnection()).thenReturn(connection);

        tripleStoreRepository.save(REPO_URL, model);

        verify(virtuosoClient).getConnection();
        verify(connection).load(REPO_URL, model);
    }

    @Test
    void shouldThrowWhenLoadingFails() {
        Model model = createSimpleModel();
        doThrow(new HttpException("Something bad happened")).when(connection).load(REPO_URL, model);
        when(virtuosoClient.getConnection()).thenReturn(connection);

        assertThatThrownBy(() -> tripleStoreRepository.save(REPO_URL, model))
            .isInstanceOf(TripleStoreRepositoryException.class)
            .hasMessage(String.format("Could not save model to '%s'", REPO_URL));

        verify(virtuosoClient).getConnection();
        verify(connection).load(REPO_URL, model);
    }

    @Test
    void shouldDeleteGraphSilently() {
        when(virtuosoClient.getSparqlEndpoint()).thenReturn("http://www.sparql.org");
        when(virtuosoClient.getHttpClient()).thenReturn(httpClient);

        try (MockedStatic<UpdateExecutionFactory> mock = mockUpdateFactory()) {
            tripleStoreRepository.clearExistingNamedGraph(REPO_URL);
            mock.verify(() -> UpdateExecutionFactory.createRemote(any(Update.class),
                eq("http://www.sparql.org"),
                eq(httpClient)));
        }

        verify(virtuosoClient, times(0)).getConnection();
        verifyNoInteractions(connection);
    }

    @Test
    void shouldThrowWhenDeletionFails() {
        when(virtuosoClient.getSparqlEndpoint()).thenReturn("http://www.sparql.org");
        when(virtuosoClient.getHttpClient()).thenReturn(httpClient);

        try (MockedStatic<UpdateExecutionFactory> mock = mockUpdateFactory()) {
            mock.when(() -> UpdateExecutionFactory.createRemote(any(Update.class), anyString(),
                any(HttpClient.class))).thenThrow(new HttpException("Something bad happened"));

            assertThatThrownBy(() -> tripleStoreRepository.clearExistingNamedGraph(REPO_URL))
                .isInstanceOf(TripleStoreRepositoryException.class);

            mock.verify(() -> UpdateExecutionFactory.createRemote(any(Update.class),
                eq("http://www.sparql.org"),
                eq(httpClient)));
        }

        verify(virtuosoClient, times(0)).getConnection();
        verifyNoInteractions(connection);
    }

    @Test
    void shouldExecuteSelect() {
        when(virtuosoClient.getConnection()).thenReturn(connection);

        tripleStoreRepository.select(new SelectBuilder());

        verify(virtuosoClient).getConnection();
        verify(connection).query(new SelectBuilder().build());
    }

    @Test
    void shouldThrowWhenExecuteSelectFails() {
        when(virtuosoClient.getConnection()).thenReturn(connection);
        when(connection.query(any(Query.class))).thenThrow(
            new HttpException("Something bad happened"));
        SelectBuilder selectBuilder = new SelectBuilder();

        assertThatThrownBy(() -> tripleStoreRepository.select(selectBuilder))
            .isInstanceOf(TripleStoreRepositoryException.class);

        verify(virtuosoClient).getConnection();
        verify(connection).query(selectBuilder.build());
    }

    private MockedStatic<UpdateExecutionFactory> mockUpdateFactory() {
        MockedStatic<UpdateExecutionFactory> mockedStatic =
            Mockito.mockStatic(UpdateExecutionFactory.class);
        mockedStatic.when(
                () -> UpdateExecutionFactory.createRemote(any(Update.class), anyString(),
                    any(HttpClient.class)))
            .thenReturn(updateProcessor);
        return mockedStatic;
    }

    private Model createSimpleModel() {
        Model model = ModelFactory.createDefaultModel();
        model.add(
            createResource("http://www.atptour.com/players/Roger_Federer"),
            RDF.type,
            createResource("https://schema.org/Person")
        );
        return model;
    }
}
