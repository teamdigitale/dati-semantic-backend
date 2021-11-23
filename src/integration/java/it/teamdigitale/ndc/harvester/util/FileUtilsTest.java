package it.teamdigitale.ndc.harvester.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilsTest {

    private FileUtils fileUtils = new FileUtils();

    // note: a temporary directory is created for each test, and will be deleted after each test.
    @TempDir
    Path rootFolder;

    @Test
    void testFolderExistence() {
        assertThat(fileUtils.folderExists(rootFolder)).isTrue();
    }

    @Test
    void textFilesAreNotFolders() throws IOException {
        File textFile = createTextFile(rootFolder, "sample.txt",
                List.of("subject: Hello World", "", "Hello, fellows!"));

        assertThat(fileUtils.isDirectory(textFile.toPath())).isFalse();
    }

    @Test
    void missingFilesAreNotFolders() {
        File missingFile = new File(rootFolder.toFile(), "does-not-exist");

        assertThat(missingFile.exists()).isFalse();
        assertThat(fileUtils.isDirectory(missingFile.toPath())).isFalse();
    }

    @Test
    void filesInFolderCanBeListed() throws IOException {
        File file1 = createTextFile(rootFolder, "text1.txt",
                List.of("subject: Hello World", "", "Hello, fellows!"));

        File file2 = createTextFile(rootFolder, "text2.txt",
                List.of("subject: Tipperary, we're coming", "", "But it's a long way to go"));

        List<Path> folderContent = fileUtils.listContents(rootFolder);
        assertThat(folderContent).contains(file1.toPath(), file2.toPath());
    }

    @Test
    void cannotListTextFileContent() throws IOException {
        File textFile = createTextFile(rootFolder, "text.txt",
                List.of("subject: Hello World", "", "Hello, fellows!"));

        assertThatThrownBy(() -> fileUtils.listContents(textFile.toPath()))
                .isInstanceOf(IOException.class);
    }

    private File createTextFile(Path parent, String fileName, List<String> content) throws IOException {
        File textFile = new File(parent.toFile(), fileName);
        Files.write(textFile.toPath(), content);
        return textFile;
    }
}