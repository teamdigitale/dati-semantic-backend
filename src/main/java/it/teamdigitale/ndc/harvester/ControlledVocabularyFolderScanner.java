package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ControlledVocabularyFolderScanner implements FolderScanner<CvPath> {
    private final FileUtils fileUtils;

    @Override
    public List<CvPath> scanFolder(Path folder) throws IOException {
        Optional<Path> ttl = fileUtils.listContents(folder).stream()
                .filter(path -> path.toString().endsWith(".ttl"))
                .findFirst();

        if (ttl.isEmpty()) {
            log.warn("Controlled vocabulary folder '{}' does not contain any TTL file", folder.toString());
            return Collections.emptyList();
        }

        String ttlPath = ttl.get().toString();

        Optional<Path> csv = fileUtils.listContents(folder).stream()
                .filter(path -> path.toString().endsWith(".csv"))
                .findFirst();

        if (csv.isPresent()) {
            return List.of(CvPath.of(csv.get().toString(), ttlPath));
        } else {
            log.info("No CSV file associated to {} in {}", ttlPath, folder);
            return List.of(CvPath.of(null, ttlPath));
        }
    }
}
