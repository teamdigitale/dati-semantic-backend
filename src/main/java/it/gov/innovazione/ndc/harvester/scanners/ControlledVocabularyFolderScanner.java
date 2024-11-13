package it.gov.innovazione.ndc.harvester.scanners;

import it.gov.innovazione.ndc.harvester.exception.InvalidAssetFolderException;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import it.gov.innovazione.ndc.harvester.util.PropertiesUtils;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticError;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;
import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticWarn;

@Component
@Slf4j
public class ControlledVocabularyFolderScanner implements FolderScanner<CvPath> {
    public static final int MIN_SKIP_WORD_LENGTH = 3;
    private final FileUtils fileUtils;
    private final List<String> lowerSkipWords;

    public ControlledVocabularyFolderScanner(FileUtils fileUtils, ControlledVocabularyFolderScannerProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Please provide valid properties");
        }
        this.fileUtils = fileUtils;
        lowerSkipWords = PropertiesUtils.lowerSkipWords(properties.getSkipWords(), MIN_SKIP_WORD_LENGTH);
    }

    @Override
    public List<CvPath> scanFolder(Path folder) throws IOException {
        Optional<Path> maybeTtl = findAtMostOne(folder, TURTLE_FILE_EXTENSION, "turtle controlled vocabulary");

        if (maybeTtl.isEmpty()) {
            logSemanticWarn(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("folder", folder.toString())
                    .message("Controlled vocabulary folder does not contain any TTL file")
                    .build());
            log.warn("Controlled vocabulary folder '{}' does not contain any TTL file", folder.toString());
            return Collections.emptyList();
        }

        String ttlPath = maybeTtl.get().toString();

        Optional<Path> maybeCsv = findAtMostOne(folder, ".csv", "flattened controlled vocabulary");

        if (maybeCsv.isPresent()) {
            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("folder", folder.toString())
                    .additionalInfo("csvPath", maybeCsv.get().toString())
                    .additionalInfo("ttlPath", ttlPath)
                    .message("Found CSV file associated to TTL file")
                    .build());
            return List.of(CvPath.of(ttlPath, maybeCsv.get().toString()));
        } else {
            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("folder", folder.toString())
                    .additionalInfo("ttlPath", ttlPath)
                    .message("No CSV file associated to TTL file")
                    .build());
            log.info("No CSV file associated to {} in {}", ttlPath, folder);
            return List.of(CvPath.of(ttlPath, null));
        }
    }

    private boolean fileNameDoesNotContainSkipWords(Path path) {
        boolean skip = lowerSkipWords.stream().noneMatch(fileUtils.getLowerCaseFileName(path)::contains);
        if (!skip) {
            logSemanticWarn(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("path", path.toString())
                    .message("Skipping file because it contains skip words")
                    .additionalInfo("skipWords", lowerSkipWords)
                    .build());
            log.info("Skipping file '{}' because it contains skip words", path);
        }
        return skip;
    }

    private Optional<Path> findAtMostOne(Path parent, String extension, String fileTypeDescription) throws IOException {
        List<Path> hits = fileUtils.listContents(parent).stream()
                .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(extension))
                .filter(this::fileNameDoesNotContainSkipWords)
                .limit(2)
                .collect(Collectors.toList());

        if (hits.size() > 1) {
            logSemanticError(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("folder", parent.toString())
                    .message("Folder contains more than one " + fileTypeDescription)
                    .build());
            log.error("Folder '{}' contains more than one {}", parent.toString(), fileTypeDescription);
            throw new InvalidAssetFolderException(String.format("Folder '%s' has more than one %s",
                    parent, fileTypeDescription));
        }

        return hits.isEmpty() ? Optional.empty() : Optional.of(hits.get(0));
    }
}
