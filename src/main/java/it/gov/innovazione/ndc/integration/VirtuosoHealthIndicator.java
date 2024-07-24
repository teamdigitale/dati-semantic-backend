package it.gov.innovazione.ndc.integration;

import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Component
@ConditionalOnEnabledHealthIndicator("virtuoso")
public class VirtuosoHealthIndicator implements HealthIndicator {
    private final TripleStoreRepository repository;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        try {
            performQuery();
            builder.up();
        } catch (QueryExceptionHTTP qeh) {
            builder.down(Objects.requireNonNullElse(qeh.getCause(), qeh));
        } catch (Exception ex) {
            builder.down(ex);
        }
        return builder.build();
    }

    private void performQuery() {
        SelectBuilder queryBuilder = new SelectBuilder()
            .addWhere("<http://www.disney.com/characters/Fethry_Duck>",
                "<http://www.w3.org/2000/01/rdf-schema#type>",
                "<http://www.disney.com/characters/Character>");

        try (QueryExecution execution = repository.select(queryBuilder)) {
            if (Objects.nonNull(execution) && execution.execSelect().hasNext()) {
                log.warn("What?! We found Fethry Duck");
            }
        }
    }
}
