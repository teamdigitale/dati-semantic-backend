package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.scanners.ControlledVocabularyFolderScanner;
import it.teamdigitale.ndc.harvester.scanners.FolderScanner;
import it.teamdigitale.ndc.harvester.scanners.OntologyFolderScanner;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import it.teamdigitale.ndc.harvester.util.GitUtils;
import it.teamdigitale.ndc.harvester.util.Version;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgencyRepositoryService {
    public static final String TEMP_DIR_PREFIX = "ndc-";
    private final FileUtils fileUtils;
    private final GitUtils gitUtils;
    private final OntologyFolderScanner ontologyFolderScanner;
    private final ControlledVocabularyFolderScanner controlledVocabularyFolderScanner;

    public Path cloneRepo(String repoUrl) throws IOException {
        Path cloneDir = fileUtils.createTempDirectory(TEMP_DIR_PREFIX);
        log.info("Cloning repo {}, at {}", repoUrl, cloneDir);
        gitUtils.cloneRepo(repoUrl, cloneDir.toFile());
        return cloneDir;
    }

    public List<CvPath> getControlledVocabularyPaths(Path clonedRepo) {
        return findPaths(clonedRepo, SemanticAssetType.CONTROLLED_VOCABULARY, controlledVocabularyFolderScanner);
    }

    public List<SemanticAssetPath> getOntologyPaths(Path clonedRepo) {
        return findPaths(clonedRepo, SemanticAssetType.ONTOLOGY, ontologyFolderScanner);
    }

    private <P extends SemanticAssetPath> List<P> findPaths(Path clonedRepo, SemanticAssetType type, FolderScanner<P> scanner) {
        Path rootFolder = Path.of(clonedRepo.toString(), type.getFolderName());
        if (!fileUtils.folderExists(rootFolder)) {
            log.warn("No {} folder found in {}", type.getDescription(), clonedRepo);
            return List.of();
        }

        return createSemanticAssetPaths(rootFolder, scanner, type.isIgnoringObsoleteVersions());
    }

    @SneakyThrows
    private <P extends SemanticAssetPath> List<P> createSemanticAssetPaths(Path dir, FolderScanner<P> scanner, boolean ignoreObsoleteVersions) {
        List<Path> dirContents = fileUtils.listContents(dir);
        boolean hasSubDir = dirContents.stream().anyMatch(fileUtils::isDirectory);
        if (!hasSubDir) {
            return scanner.scanFolder(dir);
        }

        Predicate<Path> isObsoleteVersion = p -> false;
        if (ignoreObsoleteVersions) {
            Optional<Version> maybeLatestVersion = getLatestVersion(dirContents);

            if (maybeLatestVersion.isPresent()) {
                isObsoleteVersion = isObsoleteVersionPredicate(maybeLatestVersion.get().getSourceString());
            }
        }

        return dirContents.stream()
                // consider folders for recursion
                .filter(fileUtils::isDirectory)
                // only consider folders which are not obsolete
                .filter(not(isObsoleteVersion))
                // recurse and flatten
                .flatMap(subDir -> createSemanticAssetPaths(subDir, scanner, ignoreObsoleteVersions).stream())
                // then collect
                .collect(Collectors.toList());
    }

    private Optional<Version> getLatestVersion(List<Path> dirContents) {
        return dirContents.stream()
                .map(p -> p.getFileName().toString())
                .flatMap(s -> Version.of(s).stream())
                .max(Comparator.naturalOrder());
    }

    private Predicate<Path> isObsoleteVersionPredicate(String latestVersionString) {
        return path -> {
            String fileName = path.getFileName().toString();
            boolean hasValidVersion = Version.of(fileName).isPresent();
            return hasValidVersion && !latestVersionString.equals(fileName);
        };
    }

    public void removeClonedRepo(Path repoPath) throws IOException {
        fileUtils.removeDirectory(repoPath);
    }
}
