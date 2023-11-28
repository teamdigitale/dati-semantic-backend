package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.harvester.exception.InvalidAssetFolderException;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.scanners.ControlledVocabularyFolderScanner;
import it.gov.innovazione.ndc.harvester.scanners.FolderScanner;
import it.gov.innovazione.ndc.harvester.scanners.OntologyFolderScanner;
import it.gov.innovazione.ndc.harvester.scanners.SchemaFolderScanner;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import it.gov.innovazione.ndc.harvester.util.GitUtils;
import it.gov.innovazione.ndc.harvester.util.PropertiesUtils;
import it.gov.innovazione.ndc.harvester.util.Version;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Component
@Slf4j
public class AgencyRepositoryService {
    public static final String TEMP_DIR_PREFIX = "ndc-";
    public static final int MIN_SKIP_WORD_LENGTH = 3;
    private final FileUtils fileUtils;
    private final GitUtils gitUtils;
    private final OntologyFolderScanner ontologyFolderScanner;
    private final ControlledVocabularyFolderScanner controlledVocabularyFolderScanner;
    private final SchemaFolderScanner schemaFolderScanner;
    private final List<String> lowerSkipWords;

    public AgencyRepositoryService(FileUtils fileUtils,
                                   GitUtils gitUtils,
                                   OntologyFolderScanner ontologyFolderScanner,
                                   ControlledVocabularyFolderScanner controlledVocabularyFolderScanner,
                                   SchemaFolderScanner schemaFolderScanner,
                                   AgencyRepositoryServiceProperties agencyRepositoryServiceProperties) {
        this.fileUtils = fileUtils;
        this.gitUtils = gitUtils;
        this.ontologyFolderScanner = ontologyFolderScanner;
        this.controlledVocabularyFolderScanner = controlledVocabularyFolderScanner;
        this.schemaFolderScanner = schemaFolderScanner;
        this.lowerSkipWords = PropertiesUtils.lowerSkipWords(agencyRepositoryServiceProperties.getSkipWords(), MIN_SKIP_WORD_LENGTH);
    }

    public Path cloneRepo(String repoUrl) throws IOException {
        return cloneRepo(repoUrl, null);
    }

    public Path cloneRepo(String repoUrl, String revision) throws IOException {
        Path cloneDir = fileUtils.createTempDirectory(TEMP_DIR_PREFIX);
        log.info("Cloning repo {} @ revision {}, at location {}", repoUrl, revision, cloneDir);
        gitUtils.cloneRepoAndGetRevision(repoUrl, cloneDir.toFile(), revision);
        return cloneDir;
    }

    public List<CvPath> getControlledVocabularyPaths(Path clonedRepo) {
        return findPaths(clonedRepo, SemanticAssetType.CONTROLLED_VOCABULARY, controlledVocabularyFolderScanner);
    }

    public List<SemanticAssetPath> getOntologyPaths(Path clonedRepo) {
        return findPaths(clonedRepo, SemanticAssetType.ONTOLOGY, ontologyFolderScanner);
    }

    public void removeClonedRepo(Path repoPath) throws IOException {
        fileUtils.removeDirectory(repoPath);
    }

    public List<SemanticAssetPath> getSchemaPaths(Path clonedRepoPath) {
        return findPaths(clonedRepoPath, SemanticAssetType.SCHEMA, schemaFolderScanner);
    }

    private <P extends SemanticAssetPath> List<P> findPaths(Path clonedRepo, SemanticAssetType type, FolderScanner<P> scanner) {
        Path assetRootPath = Path.of(clonedRepo.toString(), type.getFolderName());
        if (!fileUtils.folderExists(assetRootPath)) {
            log.warn("No {} folder found in {}", type.getDescription(), clonedRepo);

            assetRootPath = Path.of(clonedRepo.toString(), type.getLegacyFolderName()); 
            if (!fileUtils.folderExists(assetRootPath)) {
                return List.of();
            }
        }

        return createSemanticAssetPaths(assetRootPath, scanner, type.isIgnoringObsoleteVersions());
    }

    private boolean isDirectoryToBeSkipped(Path path) {
        String directoryName = fileUtils.getLowerCaseFileName(path);
        return fileUtils.isDirectory(path) && this.lowerSkipWords.stream().anyMatch(directoryName::contains);
    }

    @SneakyThrows
    private <P extends SemanticAssetPath> List<P> createSemanticAssetPaths(Path path, FolderScanner<P> scanner, boolean ignoreObsoleteVersions) {
        List<Path> dirContents = fileUtils.listContents(path).stream()
                .filter(c -> !isDirectoryToBeSkipped(c))
                .collect(Collectors.toList());
        boolean hasSubDir = dirContents.stream().anyMatch(fileUtils::isDirectory);
        if (!hasSubDir) {
            return tryScanDir(path, scanner);
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

    private <P extends SemanticAssetPath> List<P> tryScanDir(Path dir, FolderScanner<P> scanner) throws IOException {
        try {
            return scanner.scanFolder(dir);
        } catch (InvalidAssetFolderException e) {
            log.warn("Invalid folder {}; skipping", dir, e);
            return Collections.emptyList();
        }
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
}
