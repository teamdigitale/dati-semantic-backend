package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import it.teamdigitale.ndc.harvester.model.CvPath;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OntologyPathProcessorTest {
    @Mock
    SemanticAssetsParser semanticAssetsParser;
    @Mock
    Resource ontology;
    @InjectMocks
    OntologyPathProcessor pathProcessor;

    @Test
    void shouldProcessOntology() {
        String ttlFile = "cities.ttl";
        String csvFile = "cities.csv";
        CvPath path = CvPath.of(ttlFile, csvFile);

        when(semanticAssetsParser.getOntology(ttlFile)).thenReturn(ontology);

        pathProcessor.process(path);

        verify(semanticAssetsParser).getOntology(ttlFile);
    }
}