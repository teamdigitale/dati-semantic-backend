package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OntologyFolderScanner implements FolderScanner<SemanticAssetPath> {
    private final FileUtils fileUtils;

    @Override
    public List<SemanticAssetPath> scanFolder(Path folder) throws IOException {
        Optional<Path> ttl = fileUtils.listContents(folder).stream()
                .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".ttl"))
                // let's consider the shortest file name as the "main" one, whereas others might be aligns
                .min(Comparator.comparingInt(p -> p.toString().length()));

        return ttl
                .map(path -> new SemanticAssetPath(path.toString()))
                .stream().collect(Collectors.toList());
    }
}
