package it.teamdigitale.ndc.harvester.harvesters;

import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.exception.InvalidAssetException;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaseSemanticAssetHarvesterTest {
    private class TestHarvester extends BaseSemanticAssetHarvester<SemanticAssetPath> {
        public TestHarvester() {
            super(SemanticAssetType.ONTOLOGY);
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

    private TestHarvester harvester = new TestHarvester();
    private List<SemanticAssetPath> paths;
    @Mock
    private BiConsumer<String, SemanticAssetPath> processor;

    @Test
    void shouldMoveOnToNextOntologyIfProcessingOneFails() {
        String repoUrl = "someRepoUri";
        Path basePath = Path.of("ontologyRoot");
        SemanticAssetPath path1 = SemanticAssetPath.of("test1.ttl");
        SemanticAssetPath path2 = SemanticAssetPath.of("test2.ttl");
        paths = List.of(path1, path2);

        doThrow(new InvalidAssetException("Something went wrong")).when(processor)
                .accept(repoUrl, path1);

        harvester.harvest(repoUrl, basePath);

        verify(processor).accept(repoUrl, path1);
        verify(processor).accept(repoUrl, path2);
    }
}