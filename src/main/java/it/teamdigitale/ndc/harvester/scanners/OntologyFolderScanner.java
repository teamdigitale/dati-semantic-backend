package it.teamdigitale.ndc.harvester.scanners;

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
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OntologyFolderScanner implements FolderScanner<SemanticAssetPath> {
    private final FileUtils fileUtils;
    private final OntologyFolderScannerProperties properties;

    @Override
    public List<SemanticAssetPath> scanFolder(Path folder) throws IOException {
        List<String> skipWords = properties.getSkipWords().stream()
                .map(sw -> sw.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        Optional<Path> ttl = fileUtils.listContents(folder)
                .stream()
                // discard filenames containing stop words
                .filter(path -> {
                    String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    return skipWords.stream().noneMatch(filename::contains);
                })
                // only accept ttls
                .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(TURTLE_FILE_EXTENSION))
                // let's consider the shortest file name as the "main" one, whereas others might be aligns
                .min(Comparator.comparingInt(p -> p.toString().length()));

        return ttl
                .map(path -> SemanticAssetPath.of(path.toString()))
                .stream().collect(Collectors.toList());
    }
}
