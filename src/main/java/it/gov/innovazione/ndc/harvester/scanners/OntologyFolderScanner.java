package it.gov.innovazione.ndc.harvester.scanners;

import it.gov.innovazione.ndc.harvester.exception.InvalidAssetFolderException;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
        lowerSkipWords = extractLowerSkipWords(properties);

    }

    private List<String> extractLowerSkipWords(OntologyFolderScannerProperties properties) {
        List<String> skipWords = properties.getSkipWords();
        if (skipWords == null) {
            return Collections.emptyList();
        }
        return skipWords.stream()
                .peek(OntologyFolderScanner::checkMinSkipWordLength)
                .map(sw -> sw.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    private static void checkMinSkipWordLength(String sw) {
        if (sw.length() < MIN_SKIP_WORD_LENGTH) {
            throw new IllegalArgumentException(String.format("skip words must be at least %d characters long", MIN_SKIP_WORD_LENGTH));
        }
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

    private String getLowerCaseFileName(Path path) {
        return nonNullOrInvalidFolder(path.getFileName(), "FileName for " + path)
                .toString()
                .toLowerCase(Locale.ROOT);
    }

    private boolean isTurtleFilePath(Path path) {
        return getLowerCaseFileName(path).endsWith(TURTLE_FILE_EXTENSION);
    }

    private boolean fileNameDoesNotContainSkipWords(Path path) {
        return lowerSkipWords.stream().noneMatch(getLowerCaseFileName(path)::contains);
    }

    private <T> T nonNullOrInvalidFolder(T value, String what) {
        if (value == null) {
            throw new InvalidAssetFolderException("Found unexpected null value for " + what);
        }
        return value;
    }
}
