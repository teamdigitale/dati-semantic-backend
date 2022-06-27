package it.gov.innovazione.ndc.harvester.util;

import it.gov.innovazione.ndc.harvester.exception.InvalidAssetFolderException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.util.Comparator.reverseOrder;

@Component
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
}
