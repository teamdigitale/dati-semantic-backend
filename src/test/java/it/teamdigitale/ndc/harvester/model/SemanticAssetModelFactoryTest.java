package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticAssetModelFactoryTest {
    @Mock
    SemanticAssetsParser semanticAssetsParser;
    @Mock
    Resource controlledVocabularyResource;

    @InjectMocks
    SemanticAssetModelFactory factory;

    @Test
    void canBuildControlledVocabularyModel() {
        String ttlFile = "someFile.ttl";
        when(semanticAssetsParser.getControlledVocabulary(ttlFile)).thenReturn(controlledVocabularyResource);

        ControlledVocabularyModel model = factory.createControlledVocabulary(ttlFile);

        Resource resource = model.getMainResource();

        assertThat(resource).isNotNull();
        assertThat(resource).isSameAs(controlledVocabularyResource);
    }
}