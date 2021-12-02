package it.teamdigitale.ndc.harvester.pathprocessors;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyPathProcessorTest {
    @Mock
    SemanticAssetModelFactory modelFactory;
    @Mock
    OntologyModel ontologyModel;
    @Mock
    Resource ontology;
    @Mock
    TripleStoreRepository repository;
    @Mock
    SemanticAssetMetadataRepository metadataRepository;

    @InjectMocks
    OntologyPathProcessor pathProcessor;

    @Test
    void shouldProcessOntology() {
        String ttlFile = "cities.ttl";
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);

        when(modelFactory.createOntology(ttlFile, "some-repo")).thenReturn(ontologyModel);
        when(ontologyModel.getMainResource()).thenReturn(ontology);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(ontologyModel.extractMetadata()).thenReturn(metadata);

        pathProcessor.process("some-repo", path);

        verify(ontologyModel, atLeastOnce()).getMainResource();
        verify(modelFactory).createOntology(ttlFile, "some-repo");
        verify(ontologyModel).extractMetadata();
        verify(metadataRepository).save(metadata);
    }
}