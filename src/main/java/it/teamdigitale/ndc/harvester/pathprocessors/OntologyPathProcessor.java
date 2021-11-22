package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OntologyPathProcessor {
    private final SemanticAssetsParser semanticAssetsParser;

    public void process(SemanticAssetPath path) {
        log.info("parsing and loading file {}", path.getTtlPath());
        Resource ontology = semanticAssetsParser.getOntology(path.getTtlPath());

        log.info("Found ontology {}", ontology);
        // load the ttl file into memory

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }
}
