package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyModelTest {
    @Mock
    SemanticAssetsParser semanticAssetsParser;
    @Mock
    Resource controlledVocabulary;

    @Test
    void shouldExtractMainResource() {
        String ttlFile = "src/test/resources/testdata/cv.ttl";
        ControlledVocabularyModel model = new ControlledVocabularyModel(semanticAssetsParser, ttlFile);

        when(semanticAssetsParser.getControlledVocabulary(ttlFile)).thenReturn(controlledVocabulary);

        Resource mainResource = model.getMainResource();

        assertThat(mainResource).isSameAs(controlledVocabulary);
    }

    @Test
    void shouldExtractControlledVocabularyPathTokens() {
        String ttlFile = "src/test/resources/testdata/cv.ttl";
        ControlledVocabularyModel model = new ControlledVocabularyModel(semanticAssetsParser, ttlFile);

        when(semanticAssetsParser.getControlledVocabulary(ttlFile)).thenReturn(controlledVocabulary);
        when(semanticAssetsParser.getKeyConcept(controlledVocabulary)).thenReturn("keyConcept");
        when(semanticAssetsParser.getRightsHolderId(controlledVocabulary)).thenReturn("rightsHolder");

        assertThat(model.getKeyConcept()).isEqualTo("keyConcept");
        assertThat(model.getRightsHolderId()).isEqualTo("rightsHolder");
    }
}