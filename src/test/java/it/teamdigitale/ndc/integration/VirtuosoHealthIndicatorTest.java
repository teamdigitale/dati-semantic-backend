package it.teamdigitale.ndc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.repository.TripleStoreRepository;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class VirtuosoHealthIndicatorTest {

    private VirtuosoHealthIndicator virtuosoHealthIndicator;
    private TripleStoreRepository repository;
    private ResultSet resultSet;

    @BeforeEach
    public void beforeEach() {
        repository = mock(TripleStoreRepository.class);
        resultSet = mock(ResultSet.class);
        virtuosoHealthIndicator = new VirtuosoHealthIndicator(repository);
    }

    @Test
    void shouldReportHealthy() {
        ArgumentCaptor<SelectBuilder> queryCaptor = ArgumentCaptor.forClass(SelectBuilder.class);
        when(repository.select(queryCaptor.capture())).thenReturn(resultSet);

        Health health = virtuosoHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        //TODO : check the query
    }

    @Test
    void shouldReportDownDueToQueryException() {
        ArgumentCaptor<SelectBuilder> queryCaptor = ArgumentCaptor.forClass(SelectBuilder.class);
        when(repository.select(queryCaptor.capture())).thenThrow(
            new QueryExceptionHTTP(1, "test", new RuntimeException("random")));

        Health health = virtuosoHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error")).isEqualTo("java.lang.RuntimeException: random");
    }

    @Test
    void shouldReportDownDueToGenericException() {
        ArgumentCaptor<SelectBuilder> queryCaptor = ArgumentCaptor.forClass(SelectBuilder.class);
        when(repository.select(queryCaptor.capture())).thenThrow(new RuntimeException("random"));

        Health health = virtuosoHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error")).isEqualTo("java.lang.RuntimeException: random");
    }
}