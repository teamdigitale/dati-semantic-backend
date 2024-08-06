package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.model.SchemaModel;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import org.apache.jena.rdf.model.ResourceFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaPathProcessorTest {

    @Mock
    SchemaModel schemaModel;
    @Mock
    SemanticAssetModelFactory modelFactory;
    @Mock
    SemanticAssetMetadataRepository metadataRepository;
    @Mock
    TripleStoreRepository repository;

    @InjectMocks
    SchemaPathProcessor schemaPathProcessor;

    @Test
    void shouldLoadSchemaModel() {
        when(modelFactory.createSchema(anyString(), anyString())).thenReturn(schemaModel);

        SchemaModel actual =
            schemaPathProcessor.loadModel("index.ttl", "http://example.com/");

        verify(modelFactory).createSchema("index.ttl", "http://example.com/");
        Assertions.assertThat(actual).isEqualTo(schemaModel);
    }

    @Test
    void shouldProcessSchema() {
        String ttlFile = "index.ttl";
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);

        when(modelFactory.createSchema(ttlFile, "some-repo")).thenReturn(schemaModel);
        when(schemaModel.getMainResource()).thenReturn(
            ResourceFactory.createResource("http://example.com/schema"));
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(schemaModel.extractMetadata()).thenReturn(metadata);

        schemaPathProcessor.process("some-repo", path);

        verify(schemaModel, atLeastOnce()).getMainResource();
        verify(modelFactory).createSchema(ttlFile, "some-repo");
        verify(schemaModel).extractMetadata();
        verify(metadataRepository).save(metadata);
    }
}