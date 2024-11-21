package it.gov.innovazione.ndc.harvester.util;

import it.gov.innovazione.ndc.harvester.exception.InvalidAssetFolderException;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.RUNNING;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticWarn;
import static java.util.Comparator.reverseOrder;
import static org.apache.commons.io.FileUtils.readLines;

@Component
@Slf4j
public class FileUtils {

    public Path createTempDirectory(String tempDirPrefix) throws IOException {
        return Files.createTempDirectory(tempDirPrefix);
    }

    public List<Path> listContents(Path parent) throws IOException {
        return Files.list(parent).collect(Collectors.toList());
    }

    public boolean folderExists(Path maybeDir) {
        return maybeDir.toFile().exists() && maybeDir.toFile().isDirectory();
    }

    public boolean isDirectory(Path maybeDir) {
        return maybeDir.toFile().isDirectory();
    }

    public void removeDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public String getLowerCaseFileName(Path path) {
        return nonNullOrInvalidFolder(path.getFileName(), "FileName for " + path)
                .toString()
                .toLowerCase(Locale.ROOT);
    }

    private <T> T nonNullOrInvalidFolder(T value, String what) {
        if (value == null) {
            throw new InvalidAssetFolderException("Found unexpected null value for " + what);
        }
        return value;
    }

    public List<Repository.Maintainer> getMaintainersIfPossible(Path path) {
        // extracts maintainers from the repository, reading from README.md in root path
        if (folderExists(path)) {
            File readme = new File(path.toFile(), "README.md");
            if (readme.exists()) {
                log.info("Extracting maintainers from README.md in {}", path);
                return extractMaintainers(readme);
            }
            logSemanticWarn(LoggingContext.builder()
                    .stage(HarvesterStage.MAINTAINER_EXTRACTION)
                    .harvesterStatus(RUNNING)
                    .message("No README.md found in " + path)
                    .additionalInfo("path", path.toString())
                    .build());
            log.warn("No README.md found in {}", path);
            return List.of();
        }
        logSemanticWarn(LoggingContext.builder()
                .stage(HarvesterStage.MAINTAINER_EXTRACTION)
                .harvesterStatus(RUNNING)
                .message("Path does not exist " + path)
                .additionalInfo("path", path.toString())
                .build());
        log.warn("Path {} does not exist", path);
        return List.of();

    }

    private List<Repository.Maintainer> extractMaintainers(File readme) {
        try {
            List<String> lines = readLines(readme, Charset.defaultCharset());
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.startsWith("## Maintainers")) {
                    return extractMaintainersFromMaintainersSection(iterator);
                }
            }
        } catch (IOException e) {
            logSemanticError(LoggingContext.builder()
                    .message("Error reading README.md to extract maintainers")
                    .harvesterStatus(RUNNING)
                    .stage(HarvesterStage.MAINTAINER_EXTRACTION)
                    .details(e.getMessage())
                    .additionalInfo("path", readme.toString())
                    .build());
            log.error("Error reading README.md {}", readme, e);
        }
        return List.of();
    }

    private List<Repository.Maintainer> extractMaintainersFromMaintainersSection(Iterator<String> iterator) {
        List<Repository.Maintainer> maintainers = new ArrayList<>();
        while (iterator.hasNext()) {
            String maintainerLine = iterator.next();
            if (maintainerLine.matches("^\\s*-\\t*\\s.*")) {
                log.info("Extracting maintainer from line {}", maintainerLine);
                try {
                    maintainers.add(extractMaintainer(maintainerLine));
                } catch (Exception e) {
                    log.error("Error extracting maintainer from line {}", maintainerLine, e);
                    return maintainers;
                }
            } else {
                return maintainers;
            }
        }
        return maintainers;
    }


    private Repository.Maintainer extractMaintainer(String maintainerLine) {
        String maintainer = maintainerLine.replaceAll("^\\s*-\\t*\\s", "");
        String[] partsOne = maintainer.split(":");
        String name = partsOne[0].trim();
        String[] partsTwo = partsOne[1].split("-");
        String git = partsTwo[0].trim().replace("git (", "").replace(")", "").trim();
        String email = partsTwo[1].trim().replace("email (", "").replace(")", "").trim();
        log.info("Extracted maintainer {} {} {}", name, email, git);
        return new Repository.Maintainer(name, email, git);
    }
}
