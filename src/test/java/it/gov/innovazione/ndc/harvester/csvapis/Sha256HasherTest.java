package it.gov.innovazione.ndc.harvester.csvapis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256HasherTest {

    @Test
    void shouldHashEmptyFileToWellKnownDigest(@TempDir Path tempDir) throws IOException {
        Path empty = tempDir.resolve("empty.bin");
        Files.write(empty, new byte[0]);

        assertThat(Sha256Hasher.hashFile(empty))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void shouldHashAsciiPayloadToWellKnownDigest(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("hello.txt");
        Files.write(file, "hello".getBytes(StandardCharsets.US_ASCII));

        assertThat(Sha256Hasher.hashFile(file))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void shouldFailOnMissingFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nope.bin");

        assertThatThrownBy(() -> Sha256Hasher.hashFile(missing))
                .isInstanceOf(IOException.class);
    }
}
