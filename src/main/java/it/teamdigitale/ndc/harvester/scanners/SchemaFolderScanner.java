package it.teamdigitale.ndc.harvester.scanners;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
