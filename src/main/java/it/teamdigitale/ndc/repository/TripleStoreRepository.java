package it.teamdigitale.ndc.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
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
        RDFConnection connection = RDFConnectionFactory.connect(properties.getSparqlGraphStore().getUrl());
        try {
            saveWithConnection(graphName, model, connection);
        } finally {
            connection.close();
        }
        log.info("Model saved to Virtuoso");
    }

    private void saveWithConnection(String graphName, Model model, RDFConnection connection) {
        try {
            connection.load(graphName, model);
        } catch (HttpException e) {
            log.error("Could not flush! '{}'", e.getResponse(), e);
            throw new TripleStoreRepositoryException(String.format("Could not save model to '%s'", graphName), e);
        } catch (Exception e) {
            log.error("Could not flush!", e);
            throw new TripleStoreRepositoryException(String.format("Could not save model to '%s'", graphName), e);
        }
    }

    public void clearExistingNamedGraph(String repoUrl) {
        UpdateRequest updateRequest = UpdateFactory.create();
        updateRequest.add("CLEAR GRAPH <" + repoUrl + ">");
        UpdateExecutionFactory.createRemote(updateRequest, properties.getSparql().getUrl()).execute();
    }

    public ResultSet select(SelectBuilder selectBuilder) {
        return QueryExecutionFactory.sparqlService(properties.getSparql().getUrl(), selectBuilder.build())
                .execSelect();
    }
}
