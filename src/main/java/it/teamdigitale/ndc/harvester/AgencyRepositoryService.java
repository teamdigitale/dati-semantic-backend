package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.util.FileUtils;
import it.teamdigitale.ndc.harvester.util.GitUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AgencyRepositoryService {
    public static final String TEMP_DIR_PREFIX = "ndc-";
    public static final String CV_FOLDER = "VocabolariControllati";
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
            return Collections.emptyList();
        }
        return createCvPaths(cvFolder);
    }

    @SneakyThrows
    private List<CvPath> createCvPaths(Path dir) {
        List<CvPath> accumulator = new ArrayList<>();
        boolean hasSubDir =
            fileUtils.listContents(dir).stream().anyMatch(fileUtils::isDirectory);
        if (hasSubDir) {
            List<CvPath> subDirCvPaths = fileUtils.listContents(dir).stream()
                .map(this::createCvPaths)
                .reduce(new ArrayList<>(), (acc, cvPaths) -> {
                    acc.addAll(cvPaths);
                    return acc;
                });
            accumulator.addAll(subDirCvPaths);
        } else {
            Optional<Path> csv = fileUtils.listContents(dir).stream()
                .filter(path -> path.toString().endsWith(".csv"))
                .findFirst();

            Optional<Path> ttl = fileUtils.listContents(dir).stream()
                .filter(path -> path.toString().endsWith(".ttl"))
                .findFirst();

            if (ttl.isPresent() && csv.isPresent()) {
                accumulator.add(CvPath.of(csv.get().toString(), ttl.get().toString()));
            }
        }

        return accumulator;
    }
}
