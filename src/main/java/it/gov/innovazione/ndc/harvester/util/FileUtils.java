package it.gov.innovazione.ndc.harvester.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
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
}
