package it.teamdigitale.ndc.harvester.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
}
