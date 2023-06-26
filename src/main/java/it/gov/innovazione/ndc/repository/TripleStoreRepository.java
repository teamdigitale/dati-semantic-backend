package it.gov.innovazione.ndc.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.modify.request.UpdateDrop;
import org.apache.jena.update.UpdateExecutionFactory;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class TripleStoreRepository {

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
            throw new TripleStoreRepositoryException(
                String.format("Could not save model to '%s'", graphName), e);
        }
    }

    public void clearExistingNamedGraph(String repoUrl) {
        try {
            String sparqlEndpoint = virtuosoClient.getSparqlEndpoint();
            UpdateExecutionFactory.createRemote(new UpdateDrop(repoUrl, true), sparqlEndpoint)
                .execute();
        } catch (Exception e) {
            log.error(String.format("Could not clear existing named graph! - %s", repoUrl), e);
            throw new TripleStoreRepositoryException(
                String.format("Could not delete graph - '%s'", repoUrl), e);
        }
    }

    public QueryExecution select(SelectBuilder selectBuilder) {
        try (RDFConnection connection = virtuosoClient.getConnection()) {
            Query query = selectBuilder.build();
            return connection.query(query);
        } catch (Exception e) {
            log.error(String.format("Could not execute select! - %s", selectBuilder), e);
            throw new TripleStoreRepositoryException(
                String.format("Could not execute select - '%s'", selectBuilder), e);
        }
    }
}
