package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticAssetPathProcessorTest {
    private class TestSemanticAssetPathProcessor extends SemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {
        public TestSemanticAssetPathProcessor(TripleStoreRepository repository) {
            super(repository);
        }

        @Override
        protected OntologyModel loadModel(String ttlFile) {
            return modelDecorator;
        }
    }

    @Mock
    private OntologyModel modelDecorator;
    @Mock
    private TripleStoreRepository repository;
    @Mock
    private Model model;

    @Test
    void processingGoesThroughTwoCommonSteps() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor = new TestSemanticAssetPathProcessor(repository);
        SemanticAssetPath path = new SemanticAssetPath(ttlFile);
        when(modelDecorator.getRdfModel()).thenReturn(model);

        processor.process(repoUrl, path);

        verify(repository).save(repoUrl, model);
        verify(modelDecorator, atLeastOnce()).getMainResource();
    }
}