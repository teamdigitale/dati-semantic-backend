package it.gov.innovazione.ndc.harvester.scanners;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import it.gov.innovazione.ndc.harvester.util.PropertiesUtils;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;

@Component
public class OntologyFolderScanner implements FolderScanner<SemanticAssetPath> {
    public static final int MIN_SKIP_WORD_LENGTH = 3;
    private final FileUtils fileUtils;
    private final List<String> lowerSkipWords;

    public OntologyFolderScanner(FileUtils fileUtils, OntologyFolderScannerProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Please provide valid properties");
        }
        this.fileUtils = fileUtils;
        lowerSkipWords = PropertiesUtils.lowerSkipWords(properties.getSkipWords(), MIN_SKIP_WORD_LENGTH);

    }

    @Override
    public List<SemanticAssetPath> scanFolder(Path folder) throws IOException {
        List<SemanticAssetPath> assets = fileUtils.listContents(folder)
                .stream()
                // only accept ttls
                .filter(this::isTurtleFilePath)
                // discard filenames containing skip words
                .filter(this::fileNameDoesNotContainSkipWords)
                // transform them in SemanticAssetPaths
                .map(path -> SemanticAssetPath.of(path.toString()))
                // collect to a list
                .collect(Collectors.toList());

        logSemanticInfo(LoggingContext.builder()
                .stage(HarvesterStage.PATH_SCANNING)
                .harvesterStatus(HarvesterRun.Status.RUNNING)
                .message("Scanned folder for ontology files")
                .additionalInfo("folder", folder.toString())
                .additionalInfo("assets", assets.size())
                .build());

        return assets;

    }

    private boolean isTurtleFilePath(Path path) {
        boolean isTurtle = fileUtils.getLowerCaseFileName(path).endsWith(TURTLE_FILE_EXTENSION);
        if (!isTurtle) {
            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("file", path.toString())
                    .message("Skipping file due to extension - it's not a turtle file")
                    .build());
        }
        return isTurtle;
    }

    private boolean fileNameDoesNotContainSkipWords(Path path) {
        boolean skip = lowerSkipWords.stream().noneMatch(fileUtils.getLowerCaseFileName(path)::contains);
        if (!skip) {
            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("file", path.toString())
                    .message("Skipping file due to skip words")
                    .build());
        }
        return skip;
    }
}
