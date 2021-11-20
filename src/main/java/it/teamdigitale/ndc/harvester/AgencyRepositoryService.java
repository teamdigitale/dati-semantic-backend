package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import it.teamdigitale.ndc.harvester.util.GitUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AgencyRepositoryService {
    public static final String TEMP_DIR_PREFIX = "ndc-";
    public static final String CV_FOLDER = "VocabolariControllati";
    public static final String ONTOLOGY_FOLDER = "Ontologie";
    private final FileUtils fileUtils;
    private final GitUtils gitUtils;

    public AgencyRepositoryService(FileUtils fileUtils, GitUtils gitUtils) {
        this.fileUtils = fileUtils;
        this.gitUtils = gitUtils;
    }

    public Path cloneRepo(String repoUrl) throws IOException, GitAPIException {
        Path cloneDir = fileUtils.createTempDirectory(TEMP_DIR_PREFIX);
        log.info("Cloning repo {}, at {}", repoUrl, cloneDir);
        gitUtils.cloneRepo(repoUrl, cloneDir.toFile());
        return cloneDir;
    }

    public List<CvPath> getControlledVocabularyPaths(Path clonedRepo) {
        Path cvFolder = Path.of(clonedRepo.toString(), CV_FOLDER);
        if (!fileUtils.folderExists(cvFolder)) {
            log.warn("No controlled vocabulary folder found in {}", clonedRepo);
            return List.of();
        }
        return (List<CvPath>) (List<?>) createSemanticAssetPaths(cvFolder);
    }

    public List<SemanticAssetPath> getOntologyPaths(Path clonedRepo) {
        Path ontologyFolder = Path.of(clonedRepo.toString(), ONTOLOGY_FOLDER);
        if (!fileUtils.folderExists(ontologyFolder)) {
            log.warn("No ontology folder found in {}", clonedRepo);
            return List.of();
        }

        return createSemanticAssetPaths(ontologyFolder);
    }

    @SneakyThrows
    private List<SemanticAssetPath> createSemanticAssetPaths(Path dir) {
        boolean hasSubDir =
                fileUtils.listContents(dir).stream().anyMatch(fileUtils::isDirectory);
        if (hasSubDir) {
            return fileUtils.listContents(dir).stream()
                    .flatMap(subDir -> createSemanticAssetPaths(subDir).stream())
                    .collect(Collectors.toList());
        } else {
            Optional<Path> csv = fileUtils.listContents(dir).stream()
                    .filter(path -> path.toString().endsWith(".csv"))
                    .findFirst();

            //filter out all alignment ttls
            Optional<Path> ttl = fileUtils.listContents(dir).stream()
                    .filter(path -> path.toString().endsWith(".ttl"))
                    .findFirst();

            if (ttl.isPresent() && csv.isPresent()) {
                return List.of(CvPath.of(csv.get().toString(), ttl.get().toString()));
            } else {
                return ttl
                        .map(path -> new SemanticAssetPath(path.toString()))
                        .stream().collect(Collectors.toList());
            }
        }
    }
}
