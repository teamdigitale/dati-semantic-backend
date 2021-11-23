package it.teamdigitale.ndc.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
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
        Resource graph = ResourceFactory.createResource(graphName);
        UpdateBuilder builder = null;
        int batchSize = 0;
        StmtIterator iterator = model.listStatements();
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            if (batchSize == 0) {
                builder = new UpdateBuilder();
            }

            builder = builder.addInsert(graph, statement.asTriple());
            batchSize++;

            if (batchSize >= 20) {
                log.debug("Flushing {} statements", batchSize);
                flushBuilder(builder);
                batchSize = 0;
            }
        }

        if (batchSize > 0) {
            log.debug("Flushing {} statements", batchSize);
            flushBuilder(builder);
        }
    }

    public void clearExistingNamedGraph(String repoUrl) {
        UpdateRequest updateRequest = UpdateFactory.create();
        updateRequest.add("CLEAR GRAPH <" + repoUrl + ">");
        UpdateExecutionFactory.createRemote(updateRequest, properties.getUrl()).execute();
    }

    private void flushBuilder(UpdateBuilder builder) {
        UpdateRequest updateRequest = builder.buildRequest();
        try {
            UpdateExecutionFactory.createRemote(updateRequest, properties.getUrl()).execute();
        } catch (HttpException e) {
            log.error("Could not flush! '{}'", e.getResponse(), e);
        }
    }
}
