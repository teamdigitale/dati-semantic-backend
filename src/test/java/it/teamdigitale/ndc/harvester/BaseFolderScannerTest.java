package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.util.FileUtils;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.mockito.Mockito.when;

public class BaseFolderScannerTest {
    protected final Path folder = Path.of("/tmp/fake");

    @Mock
    FileUtils fileUtils;

    protected void mockFolderToContain(String... files) throws IOException {
        when(fileUtils.listContents(folder)).thenReturn(
                stream(files)
                        .map(Path::of)
                        .collect(Collectors.toList()
                        )
        );
    }
}
