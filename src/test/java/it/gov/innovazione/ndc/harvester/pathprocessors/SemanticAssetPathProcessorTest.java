package it.gov.innovazione.ndc.harvester.pathprocessors;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;
import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.exception.SinglePathProcessingException;
import it.gov.innovazione.ndc.harvester.model.OntologyModel;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelValidationContext;
import it.gov.innovazione.ndc.harvester.model.exception.InvalidModelException;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.harvester.service.SemanticContentStatsService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidationResult;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepositoryException;
import org.apache.jena.rdf.model.Model;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticAssetPathProcessorTest {
    private class TestSemanticAssetPathProcessor
        extends BaseSemanticAssetPathProcessor<SemanticAssetPath, OntologyModel> {
        public TestSemanticAssetPathProcessor(TripleStoreRepository tripleStoreRepository,
                                              SemanticAssetMetadataRepository metadataRepository,
                                              RdfSyntaxValidator rdfSyntaxValidator) {
            super(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        }

        @Override
        protected OntologyModel loadModel(String ttlFile, String repoUrl) {
            return modelDecorator;
        }

        @Override
        protected void enrichModelBeforePersisting(OntologyModel model, SemanticAssetPath path) {
            modelEnricher.accept(model);
        }

        @Override
        protected SemanticAssetModelValidationContext validateMetadataForReport(String ttlFile, String repoUrl) {
            return SemanticAssetModelValidationContext.getForValidation();
        }

        @Override
        protected SemanticAssetType getAssetType() {
            return SemanticAssetType.ONTOLOGY;
        }
    }

    @Mock
    private OntologyModel modelDecorator;
    @Mock
    private TripleStoreRepository tripleStoreRepository;
    @Mock
    private SemanticAssetMetadataRepository metadataRepository;
    @Mock
    private RdfSyntaxValidator rdfSyntaxValidator;
    @Mock
    private Model model;
    @Mock
    SemanticContentStatsService semanticContentStatsService;
    @Mock
    private Consumer<OntologyModel> modelEnricher;

    private static final RdfSyntaxValidationResult VALID_RESULT =
            RdfSyntaxValidationResult.builder().build();

    @Test
    void processingGoesThroughTwoCommonSteps() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(VALID_RESULT);
        when(modelDecorator.getRdfModel()).thenReturn(model);
        when(modelDecorator.extractMetadata()).thenReturn(metadata);

        processor.process(repoUrl, path);

        InOrder inOrder =
            Mockito.inOrder(tripleStoreRepository, modelDecorator, metadataRepository);

        inOrder.verify(modelDecorator).extractMetadata();
        inOrder.verify(metadataRepository).save(metadata);
        inOrder.verify(tripleStoreRepository).save(repoUrl, model);
    }

    @Test
    void ifModelCannotBeStoredShouldStopProcessingAndPropagate() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(VALID_RESULT);
        when(modelDecorator.getRdfModel()).thenReturn(model);
        TripleStoreRepositoryException repositoryException =
            new TripleStoreRepositoryException("Oops!");
        doThrow(repositoryException).when(tripleStoreRepository).save(repoUrl, model);

        assertThatThrownBy(() -> processor.process(repoUrl, path))
            .isInstanceOf(SinglePathProcessingException.class)
                .has(new Condition<>(e -> e.getCause().getCause() == repositoryException, "cause is the expected exception"))
                .has(new Condition<>(e -> ((SinglePathProcessingException) e).isFatal(), "is fatal"));

        verify(tripleStoreRepository).save(repoUrl, model);
        verify(metadataRepository, never()).save(metadata);
    }

    @Test
    void ifModelCannotBeLoadedShouldStopProcessingAndPropagate() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            spy(new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator));
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);
        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(VALID_RESULT);
        InvalidModelException invalidModelException =
            new InvalidModelException("Cannot load model", new RuntimeException("Malformed TTL"));
        when(processor.loadModel(ttlFile, repoUrl)).thenThrow(invalidModelException);

        assertThatThrownBy(() -> processor.process(repoUrl, path))
            .isInstanceOf(SinglePathProcessingException.class)
            .hasCause(invalidModelException);

        verifyNoInteractions(tripleStoreRepository);
        verifyNoInteractions(metadataRepository);
    }

    @Test
    void ifMainResourceCannotBeExtractedShouldStopProcessingAndPropagate() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);
        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(VALID_RESULT);
        InvalidModelException invalidModelException =
            new InvalidModelException("Cannot find main resource");
        when(modelDecorator.getMainResource()).thenThrow(invalidModelException);

        assertThatThrownBy(() -> processor.process(repoUrl, path))
            .isInstanceOf(SinglePathProcessingException.class)
            .hasCause(invalidModelException);

        verifyNoInteractions(tripleStoreRepository);
        verifyNoInteractions(metadataRepository);
    }

    @Test
    void ifModelCannotBeEnrichedShouldStopProcessingAndPropagate() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);
        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(VALID_RESULT);
        RuntimeException enrichmentException =
            new RuntimeException("Something went wrong calculating enrichments");
        doThrow(enrichmentException).when(modelEnricher).accept(modelDecorator);

        assertThatThrownBy(() -> processor.process(repoUrl, path))
            .isInstanceOf(SinglePathProcessingException.class)
            .hasCause(enrichmentException);

        verifyNoInteractions(tripleStoreRepository);
        verifyNoInteractions(metadataRepository);
    }

    @Test
    void ifSyntaxValidationFailsShouldStopProcessingAndPropagate() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);

        RdfSyntaxValidationResult failedResult = RdfSyntaxValidationResult.builder()
                .error(RdfSyntaxValidationResult.Issue.builder()
                        .line(5).col(10)
                        .message("Expected '.' or '}', got KEYWORD")
                        .build())
                .build();
        when(rdfSyntaxValidator.validateTurtle(ttlFile)).thenReturn(failedResult);

        assertThatThrownBy(() -> processor.process(repoUrl, path))
            .isInstanceOf(SinglePathProcessingException.class)
            .has(new Condition<>(e -> !((SinglePathProcessingException) e).isFatal(), "is not fatal"))
            .cause()
            .isInstanceOf(SinglePathProcessingException.class)
            .hasMessageContaining("Turtle syntax validation failed")
            .hasMessageContaining("Expected '.' or '}', got KEYWORD");

        verifyNoInteractions(tripleStoreRepository);
        verifyNoInteractions(metadataRepository);
    }

    @Test
    void ifSyntaxValidationPassesWithWarningsProcessingShouldContinue() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator);
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();

        RdfSyntaxValidationResult warningResult = RdfSyntaxValidationResult.builder()
                .warning(RdfSyntaxValidationResult.Issue.builder()
                        .line(3).col(1)
                        .message("Prefix 'ex' is unused")
                        .build())
                .build();
        when(rdfSyntaxValidator.validateTurtle(ttlFile)).thenReturn(warningResult);
        when(modelDecorator.getRdfModel()).thenReturn(model);
        when(modelDecorator.extractMetadata()).thenReturn(metadata);

        processor.process(repoUrl, path);

        verify(metadataRepository).save(metadata);
        verify(tripleStoreRepository).save(repoUrl, model);
    }

    @Test
    void shouldInvokeShadowValidationWithoutAffectingPersistenceFlow() {
        final String repoUrl = "https://github.com/italia/daf-ontologie-vocabolari-controllati";
        String ttlFile = "somefile.ttl";
        TestSemanticAssetPathProcessor processor =
            spy(new TestSemanticAssetPathProcessor(tripleStoreRepository, metadataRepository, rdfSyntaxValidator));
        SemanticAssetPath path = SemanticAssetPath.of(ttlFile);
        SemanticAssetMetadata metadata = SemanticAssetMetadata.builder().build();
        when(rdfSyntaxValidator.validateTurtle(anyString())).thenReturn(VALID_RESULT);
        when(modelDecorator.getRdfModel()).thenReturn(model);
        when(modelDecorator.extractMetadata()).thenReturn(metadata);

        HarvestExecutionContextUtils.setContext(HarvestExecutionContext.builder()
                .repository(Repository.builder().id("repo-id").url(repoUrl).active(true).build())
                .runId("run-id")
                .correlationId("corr-id")
                .currentUserId("user")
                .rootPath("")
                .instance(Instance.PRIMARY)
                .build());
        try {
            processor.process(repoUrl, path);
        } finally {
            HarvestExecutionContextUtils.clearContext();
        }

        verify(processor).validateMetadataForReport(ttlFile, repoUrl);
        verify(metadataRepository).save(metadata);
        verify(tripleStoreRepository).save(repoUrl, model);
    }
}
