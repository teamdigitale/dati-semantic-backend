package it.teamdigitale.ndc.repository;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripleStoreRepositoryTest {
    private static final String REPO_URL = "http://www.repos.org/reponame";
    public static final TripleStoreRepositoryProperties REPO_PROPERTIES =
        TripleStoreRepositoryProperties.forAnonymousBaseUrl("http://localhost/");

    private final TripleStoreRepository repository = new TripleStoreRepository(REPO_PROPERTIES);

    @Mock
    RDFConnection connection;

    @Test
    void shouldConnectAndLoadModelWhenSaving() {
        Model model = createSimpleModel();

        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {

            repository.save(REPO_URL, model);

            connectionFactoryMockedStatic.verify(
                () -> RDFConnectionFactory.connect(anyString(), anyString(), anyString()));
        }

        verify(connection).load(REPO_URL, model);
    }

    @Test
    void shouldThrowWhenLoadingFailsWithHttpException() {
        Model model = createSimpleModel();
        doThrow(new HttpException("Something bad happened")).when(connection).load(REPO_URL, model);

        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {
            assertThatThrownBy(() -> repository.save(REPO_URL, model))
                .isInstanceOf(TripleStoreRepositoryException.class);
        }
    }

    @Test
    void shouldThrowWhenLoadingFailsWithGenericException() {
        Model model = createSimpleModel();
        doThrow(new RuntimeException("Something bad happened")).when(connection)
            .load(REPO_URL, model);

        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {
            assertThatThrownBy(() -> repository.save(REPO_URL, model))
                .isInstanceOf(TripleStoreRepositoryException.class);
        }
    }

    @Test
    void shouldDeleteGraphWhenExists() {
        when(connection.queryAsk(anyString())).thenReturn(true);
        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {

            repository.clearExistingNamedGraph(REPO_URL);

            connectionFactoryMockedStatic.verify(
                () -> RDFConnectionFactory.connect(anyString(), anyString(), anyString()));
        }

        verify(connection).delete(REPO_URL);
    }

    @Test
    void shouldNotDeleteGraphWhenNotExists() {
        when(connection.queryAsk(anyString())).thenReturn(false);
        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {

            repository.clearExistingNamedGraph(REPO_URL);

            connectionFactoryMockedStatic.verify(
                () -> RDFConnectionFactory.connect(anyString(), anyString(), anyString()));
        }

        verify(connection, times(0)).delete(REPO_URL);
    }

    @Test
    void shouldThrowWhenDeletionFails() {
        when(connection.queryAsk(anyString())).thenReturn(true);

        doThrow(new RuntimeException("Something bad happened")).when(connection).delete(REPO_URL);

        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {
            assertThatThrownBy(() -> repository.clearExistingNamedGraph(REPO_URL))
                .isInstanceOf(TripleStoreRepositoryException.class);
        }
    }

    @Test
    void shouldExecuteSelect() {
        QueryExecution queryExecution = mock(QueryExecution.class);
        SelectBuilder selectBuilder = new SelectBuilder();
        when(connection.query(selectBuilder.build())).thenReturn(queryExecution);
        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {

            repository.select(selectBuilder);

            connectionFactoryMockedStatic.verify(
                () -> RDFConnectionFactory.connect(anyString(), anyString(), anyString()));
        }

        verify(connection).query(selectBuilder.build());
    }

    @Test
    void shouldThrowWhenExecuteSelectFails() {
        doThrow(new RuntimeException("Something bad happened")).when(connection)
            .query(any(Query.class));

        try (
            MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {
            assertThatThrownBy(() -> repository.select(new SelectBuilder()))
                .isInstanceOf(TripleStoreRepositoryException.class);
        }
    }

    private MockedStatic<RDFConnectionFactory> mockConnectionFactory() {
        MockedStatic<RDFConnectionFactory> mockedStatic =
            Mockito.mockStatic(RDFConnectionFactory.class);
        mockedStatic.when(() -> RDFConnectionFactory.connect(anyString(), anyString(), anyString()))
            .thenReturn(connection);
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