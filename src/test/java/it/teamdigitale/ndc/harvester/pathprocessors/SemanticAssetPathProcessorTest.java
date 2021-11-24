package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.exception.SinglePathProcessingException;
import it.teamdigitale.ndc.harvester.model.OntologyModel;
import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepositoryException;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SemanticAssetPathProcessorTest {
    private class TestSemanticAssetPathProcessor extends SemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {
        public TestSemanticAssetPathProcessor(TripleStoreRepository tripleStoreRepository, SemanticAssetMetadataRepository metadataRepository) {
            super(tripleStoreRepository, metadataRepository);
        }

        @Override
        protected OntologyModel loadModel(String ttlFile) {
            return modelDecorator;
        }
    }

    @Mock
    private OntologyModel modelDecorator;
    @Mock
    private TripleStoreRepository tripleStoreRepository;
    @Mock
    private SemanticAssetMetadataRepository metadataRepository;
    @Mock
    private Model model;

    @Test
    void processingGoesThroughTwoCommonSteps() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor = new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository);
        SemanticAssetPath path = new SemanticAssetPath(ttlFile);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(modelDecorator.getRdfModel()).thenReturn(model);
        when(modelDecorator.extractMetadata()).thenReturn(metadata);

        processor.process(repoUrl, path);

        verify(tripleStoreRepository).save(repoUrl, model);
        verify(modelDecorator, atLeastOnce()).getMainResource();
        verify(modelDecorator).extractMetadata();
        verify(metadataRepository).save(metadata);
    }

    @Test
    void ifModelCannotBeStoredShouldStopProcessingAndPropagate() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor = new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository);
        SemanticAssetPath path = new SemanticAssetPath(ttlFile);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(modelDecorator.getRdfModel()).thenReturn(model);
        TripleStoreRepositoryException repositoryException = new TripleStoreRepositoryException("Oops!");
        doThrow(repositoryException).when(tripleStoreRepository).save(repoUrl, model);

        assertThatThrownBy(() -> processor.process(repoUrl, path))
                .isInstanceOf(SinglePathProcessingException.class)
                .hasCause(repositoryException);

        verify(tripleStoreRepository).save(repoUrl, model);
        verify(modelDecorator, atLeastOnce()).getMainResource();
        verify(modelDecorator, never()).extractMetadata();
        verify(metadataRepository, never()).save(metadata);

    }
}