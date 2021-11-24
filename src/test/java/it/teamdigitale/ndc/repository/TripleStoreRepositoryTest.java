package it.teamdigitale.ndc.repository;

import org.apache.jena.atlas.web.HttpException;
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

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripleStoreRepositoryTest {
    private static final String REPO_URL = "http://www.repos.org/reponame";
    public static final TripleStoreRepositoryProperties REPO_PROPERTIES = TripleStoreRepositoryProperties.forAnonymousBaseUrl("http://localhost/");

    private final TripleStoreRepository repository = new TripleStoreRepository(REPO_PROPERTIES);

    @Mock
    RDFConnection connection;

    @Test
    void shouldConnectAndLoadModelWhenSaving() {
        Model model = createSimpleModel();

        try (MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {

            repository.save(REPO_URL, model);

            connectionFactoryMockedStatic.verify(() -> RDFConnectionFactory.connect(anyString()));
        }

        verify(connection).load(REPO_URL, model);
    }

    @Test
    void shouldThrowWhenLoadingFailsWithHttpException() {
        Model model = createSimpleModel();
        doThrow(new HttpException("Something bad happened")).when(connection).load(REPO_URL, model);

        try (MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {
            assertThatThrownBy(() -> repository.save(REPO_URL, model))
                    .isInstanceOf(TripleStoreRepositoryException.class);
        }
    }

    @Test
    void shouldThrowWhenLoadingFailsWithGenericException() {
        Model model = createSimpleModel();
        doThrow(new RuntimeException("Something bad happened")).when(connection).load(REPO_URL, model);

        try (MockedStatic<RDFConnectionFactory> connectionFactoryMockedStatic = mockConnectionFactory()) {
            assertThatThrownBy(() -> repository.save(REPO_URL, model))
                    .isInstanceOf(TripleStoreRepositoryException.class);
        }
    }

    private MockedStatic<RDFConnectionFactory> mockConnectionFactory() {
        MockedStatic<RDFConnectionFactory> mockedStatic = Mockito.mockStatic(RDFConnectionFactory.class);
        mockedStatic.when(() -> RDFConnectionFactory.connect(anyString())).thenReturn(connection);
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