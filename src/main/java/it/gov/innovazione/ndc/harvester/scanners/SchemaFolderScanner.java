package it.gov.innovazione.ndc.harvester.scanners;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SchemaFolderScanner implements FolderScanner<SemanticAssetPath> {
    private final FileUtils fileUtils;

    @Override
    public List<SemanticAssetPath> scanFolder(Path folder) throws IOException {
        return fileUtils.listContents(folder).stream()
            .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith("/index.ttl"))
            .map(path -> SemanticAssetPath.of(path.toString()))
            .collect(Collectors.toList());
    }
}
