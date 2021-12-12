package it.teamdigitale.ndc.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class TripleStoreRepository {
    private final TripleStoreRepositoryProperties properties;

    public TripleStoreRepository(TripleStoreRepositoryProperties properties) {
        this.properties = properties;
    }

    public void save(String graphName, Model model) {
        log.info("Saving model to Virtuoso");
        try (RDFConnection connection = getConnection()) {
            saveWithConnection(graphName, model, connection);
        }
        log.info("Model saved to Virtuoso");
    }

    private void saveWithConnection(String graphName, Model model, RDFConnection connection) {
        try {
            connection.load(graphName, model);
        } catch (HttpException e) {
            log.error("Could not flush! '{}'", e.getResponse(), e);
            throw new TripleStoreRepositoryException(
                String.format("Could not save model to '%s'", graphName), e);
        } catch (Exception e) {
            log.error("Could not flush!", e);
            throw new TripleStoreRepositoryException(
                String.format("Could not save model to '%s'", graphName), e);
        }
    }

    public void clearExistingNamedGraph(String repoUrl) {
        try (RDFConnection connection = getConnection()) {
            boolean graphExists = connection.queryAsk(
                String.format("ASK WHERE { GRAPH <%s> { ?s ?p ?o } }", repoUrl));
            if (graphExists) {
                connection.delete(repoUrl);
            }
        } catch (Exception e) {
            log.error(String.format("Could not clear existing named graph! - %s", repoUrl), e);
            throw new TripleStoreRepositoryException(
                String.format("Could not delete graph - '%s'", repoUrl), e);
        }
    }

    public QueryExecution select(SelectBuilder selectBuilder) {
        try (RDFConnection connection = getConnection()) {
            return connection.query(selectBuilder.build());
        } catch (Exception e) {
            log.error(String.format("Could not execute select! - %s", selectBuilder), e);
            throw new TripleStoreRepositoryException(
                String.format("Could not execute select - '%s'", selectBuilder), e);
        }
    }

    private RDFConnection getConnection() {
        String sparql = properties.getSparql().getUrl();
        String graphProtocolUrl = properties.getSparqlGraphStore().getUrl();
        RDFConnection connection = RDFConnectionFactory.connect(sparql, sparql, graphProtocolUrl);
        if (connection == null) {
            throw new TripleStoreRepositoryException("Could not connect to Virtuoso");
        }
        return connection;
    }
}
