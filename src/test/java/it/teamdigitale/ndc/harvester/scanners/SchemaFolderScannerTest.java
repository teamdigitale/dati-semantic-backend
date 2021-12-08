package it.teamdigitale.ndc.harvester.scanners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaFolderScannerTest {


    @Test
    void shouldGetIndexTtlFile() throws IOException {
        FileUtils fileUtils = mock(FileUtils.class);
        SchemaFolderScanner scanner = new SchemaFolderScanner(fileUtils);
        when(fileUtils.listContents(Path.of("/tmp/schemas")))
            .thenReturn(List.of(
                Path.of("/tmp/schemas/index.ttl"),
                Path.of("/tmp/schemasSomeIndex.ttl")));

        List<SemanticAssetPath> semanticAssetPaths = scanner.scanFolder(Path.of("/tmp/schemas"));

        assertThat(semanticAssetPaths).hasSize(1);
        assertThat(semanticAssetPaths).containsExactlyInAnyOrder(
            SemanticAssetPath.of("/tmp/schemas/index.ttl"));
    }
}