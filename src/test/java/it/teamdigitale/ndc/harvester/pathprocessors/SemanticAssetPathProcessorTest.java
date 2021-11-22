package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SemanticAssetPathProcessorTest {
    private class TestSemanticAssetPathProcessor extends SemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {
        @Override
        protected OntologyModel loadModel(String ttlFile) {
            return modelDecorator;
        }
    }

    @Mock
    private OntologyModel modelDecorator;

    @Test
    void processingGoesThroughTwoCommonSteps() {
        String ttlFile = "somefile.ttl";

        TestSemanticAssetPathProcessor processor = new TestSemanticAssetPathProcessor();

        SemanticAssetPath path = new SemanticAssetPath(ttlFile);

        processor.process(path);

        verify(modelDecorator, atLeastOnce()).getMainResource();
    }
}