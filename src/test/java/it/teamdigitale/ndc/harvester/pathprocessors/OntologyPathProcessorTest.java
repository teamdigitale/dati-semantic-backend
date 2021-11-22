package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OntologyPathProcessorTest {
    @Mock
    SemanticAssetModelFactory modelFactory;
    @Mock
    OntologyModel ontologyModel;
    @Mock
    Resource ontology;
    @InjectMocks
    OntologyPathProcessor pathProcessor;

    @Test
    void shouldProcessOntology() {
        String ttlFile = "cities.ttl";
        SemanticAssetPath path = new SemanticAssetPath(ttlFile);

        when(modelFactory.createOntology(ttlFile)).thenReturn(ontologyModel);
        when(ontologyModel.getMainResource()).thenReturn(ontology);

        pathProcessor.process(path);

        verify(ontologyModel, atLeastOnce()).getMainResource();
        verify(modelFactory).createOntology(ttlFile);
    }
}