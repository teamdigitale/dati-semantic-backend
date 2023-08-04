package it.gov.innovazione.ndc.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.UpdateExecution;
import org.springframework.stereotype.Repository;

import static java.lang.String.format;

@Slf4j
@Repository
public class TripleStoreRepository {
    private static final String DROP_SILENT_GRAPH_WITH_LOG_ENABLE_3 = "DEFINE sql:log-enable 3%nDROP SILENT GRAPH <%s>%n";

    private final VirtuosoClient virtuosoClient;

    public TripleStoreRepository(VirtuosoClient virtuosoClient) {
        this.virtuosoClient = virtuosoClient;
    }

    public void save(String graphName, Model model) {
        log.info("Saving model to Virtuoso");
        try (RDFConnection connection = virtuosoClient.getConnection()) {
            saveWithConnection(graphName, model, connection);
        }
        log.info("Model saved to Virtuoso");
    }

    private void saveWithConnection(String graphName, Model model, RDFConnection connection) {
        try {
            connection.load(graphName, model);
        } catch (Exception e) {
            log.error("Could not flush!", e);
            throw new TripleStoreRepositoryException(format("Could not save model to '%s'", graphName), e);
        }
    }

    public void clearExistingNamedGraph(String repoUrl) {
        try {
            String sparqlEndpoint = virtuosoClient.getSparqlEndpoint();
            UpdateExecution.service(sparqlEndpoint).updateString(getUpdateCommand(repoUrl)).execute();
        } catch (Exception e) {
            log.error(format("Could not clear existing named graph! - %s", repoUrl), e);
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                log.error("HttpException: {}", httpException.getResponse());
            }
            throw new TripleStoreRepositoryException(format("Could not delete graph - '%s'", repoUrl), e);
        }
    }

    private static String getUpdateCommand(String repoUrl) {
        return format(DROP_SILENT_GRAPH_WITH_LOG_ENABLE_3, repoUrl);
    }

    public QueryExecution select(SelectBuilder selectBuilder) {
        try (RDFConnection connection = virtuosoClient.getConnection()) {
            Query query = selectBuilder.build();
            return connection.query(query);
        } catch (Exception e) {
            log.error(format("Could not execute select! - %s", selectBuilder), e);
            throw new TripleStoreRepositoryException(format("Could not execute select - '%s'", selectBuilder), e);
        }
    }
}
