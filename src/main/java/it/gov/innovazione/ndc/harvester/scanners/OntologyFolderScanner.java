package it.gov.innovazione.ndc.harvester.scanners;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import it.gov.innovazione.ndc.harvester.util.PropertiesUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
        return fileUtils.listContents(folder)
                .stream()
                // only accept ttls
                .filter(this::isTurtleFilePath)
                // discard filenames containing skip words
                .filter(this::fileNameDoesNotContainSkipWords)
                // transform them in SemanticAssetPaths
                .map(path -> SemanticAssetPath.of(path.toString()))
                // collect to a list
                .collect(Collectors.toList());
    }

    private boolean isTurtleFilePath(Path path) {
        return fileUtils.getLowerCaseFileName(path).endsWith(TURTLE_FILE_EXTENSION);
    }

    private boolean fileNameDoesNotContainSkipWords(Path path) {
        return lowerSkipWords.stream().noneMatch(fileUtils.getLowerCaseFileName(path)::contains);
    }
}
