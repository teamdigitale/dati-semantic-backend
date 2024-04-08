package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.config.HarvestExecutionContext;
import it.gov.innovazione.ndc.config.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.exception.InvalidAssetException;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.service.ConfigService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseSemanticAssetHarvesterTest {
    @Mock
    private NdcEventPublisher eventPublisher;
    @Mock
    private ConfigService configService;

    @Test
    void shouldMoveOnToNextOntologyIfProcessingOneFails() {
        String repoUrl = "someRepoUri";
        Path basePath = Path.of("ontologyRoot");
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");
        SemanticAssetPath path2 = SemanticAssetPath.of("test2.ttl");
        List<SemanticAssetPath> paths = List.of(path1, path2);

        TestHarvester harvester = new TestHarvester(paths);

        doThrow(new InvalidAssetException("Something went wrong")).when(processor)
                .accept(repoUrl, path1);

        harvester.harvest(asRepo(repoUrl), basePath);

        verify(processor).accept(repoUrl, path1);
        verify(processor).accept(repoUrl, path2);
    }

    @Mock
    private BiConsumer<String, SemanticAssetPath> processor;

    @Test
    void shouldNotifyIfSizeExceed() {
        String repoUrl = "someRepoUri";
        Path basePath = Path.of("ontologyRoot");
        SemanticAssetPath path = mock(SemanticAssetPath.class);

        File file = mock(File.class);
        when(path.getAllFiles()).thenReturn(List.of(file));
        when(path.getTtlPath()).thenReturn("someRootPath/someFile.ttl");
        when(file.length()).thenReturn(2L);

        Repository repository = asRepo(repoUrl).toBuilder()
                .maxFileSizeBytes(1L)
                .build();

        doNothing().when(processor).accept(repoUrl, path);

        try (MockedStatic<HarvestExecutionContextUtils> contextUtils = mockStatic(HarvestExecutionContextUtils.class)) {
            contextUtils.when(HarvestExecutionContextUtils::getContext)
                    .thenReturn(
                            HarvestExecutionContext.builder()
                                    .repository(repository)
                                    .revision("someRevision")
                                    .correlationId("someCorrelationId")
                                    .runId("someRunId")
                                    .currentUserId("someUserId")
                                    .rootPath("someRootPath")
                                    .build());

            TestHarvester harvester = new TestHarvester(List.of(path));

            harvester.harvest(repository, basePath);
            verify(eventPublisher).publishEvent(
                    anyString(),
                    anyString(),
                    anyString(),
                    anyString(),
                    any());
        }
    }

    private class TestHarvester extends BaseSemanticAssetHarvester<SemanticAssetPath> {

        private final List<SemanticAssetPath> paths;

        public TestHarvester(List<SemanticAssetPath> paths) {
            super(SemanticAssetType.ONTOLOGY, eventPublisher, configService);
            this.paths = paths;
        }

        @Override
        protected void processPath(String repoUrl, SemanticAssetPath path) {
            processor.accept(repoUrl, path);
        }

        @Override
        protected List<SemanticAssetPath> scanForPaths(Path rootPath) {
            return paths;
        }
    }
}
