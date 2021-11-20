package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OntologyFolderScanner implements FolderScanner<SemanticAssetPath> {
    private final FileUtils fileUtils;

    @Override
    public List<SemanticAssetPath> scanFolder(Path folder) throws IOException {
        //filter out all alignment ttls
        Optional<Path> ttl = fileUtils.listContents(folder).stream()
                .filter(path -> path.toString().endsWith(".ttl"))
                .findFirst();

        return ttl
                .map(path -> new SemanticAssetPath(path.toString()))
                .stream().collect(Collectors.toList());
    }
}
