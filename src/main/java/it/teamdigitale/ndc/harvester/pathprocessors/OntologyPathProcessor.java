package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OntologyPathProcessor {
    private final SemanticAssetModelFactory modelFactory;

    public void process(SemanticAssetPath path) {
        String ttlPath = path.getTtlPath();
        log.info("parsing and loading file {}", ttlPath);

        OntologyModel ontologyModel = modelFactory.createOntology(ttlPath);
        Resource ontology = ontologyModel.getMainResource();

        log.info("Found ontology {}", ontology);
        // load the ttl file into memory

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }
}
