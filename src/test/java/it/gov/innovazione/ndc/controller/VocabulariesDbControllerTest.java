package it.gov.innovazione.ndc.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class VocabulariesDbControllerTest {

    private static final long CACHE_MAX_AGE = 86_400L;
    private static final String SQLITE_CONTENT_TYPE = "application/vnd.sqlite3";

    @Test
    void serves200WithSqliteHeadersWhenFileExists(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "fake-sqlite".getBytes(StandardCharsets.UTF_8));

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);
        ResponseEntity<Resource> response = controller.serve(new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType(SQLITE_CONTENT_TYPE));
        assertThat(response.getHeaders().getContentLength()).isEqualTo(Files.size(file));
        assertThat(response.getHeaders().getETag()).startsWith("\"").endsWith("\"");
        // HTTP-date precision is seconds; Spring truncates millis to second boundary.
        assertThat(response.getHeaders().getLastModified() / 1000)
                .isEqualTo(Files.getLastModifiedTime(file).toMillis() / 1000);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=86400, public");
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void returns404WhenFileMissing(@TempDir Path tempDir) throws IOException {
        Path missing = tempDir.resolve("not-there.db");

        VocabulariesDbController controller = new VocabulariesDbController(missing.toString(), CACHE_MAX_AGE);
        ResponseEntity<Resource> response = controller.serve(new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void returns304WhenIfNoneMatchEqualsEtag(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "fake-sqlite".getBytes(StandardCharsets.UTF_8));

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);
        String etag = Objects.requireNonNull(
                controller.serve(new MockHttpServletRequest()).getHeaders().getETag());

        MockHttpServletRequest conditional = new MockHttpServletRequest();
        conditional.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
        ResponseEntity<Resource> response = controller.serve(conditional);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(response.getBody()).isNull();
        assertThat(response.getHeaders().getETag()).isEqualTo(etag);
    }

    @Test
    void returns200WhenIfNoneMatchDiffers(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "fake-sqlite".getBytes(StandardCharsets.UTF_8));

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(HttpHeaders.IF_NONE_MATCH, "\"some-other-etag\"");

        ResponseEntity<Resource> response = controller.serve(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void returns304WhenIfModifiedSinceCoversLastModified(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "fake-sqlite".getBytes(StandardCharsets.UTF_8));
        long lastModified = Files.getLastModifiedTime(file).toMillis();

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(HttpHeaders.IF_MODIFIED_SINCE, lastModified);
        ResponseEntity<Resource> response = controller.serve(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void etagIsStableAcrossRequestsForSameFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "fake-sqlite".getBytes(StandardCharsets.UTF_8));

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);
        String first = controller.serve(new MockHttpServletRequest()).getHeaders().getETag();
        String second = controller.serve(new MockHttpServletRequest()).getHeaders().getETag();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void etagComesFromSidecarWhenAvailable(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "fake-sqlite".getBytes(StandardCharsets.UTF_8));
        Path sidecar = tempDir.resolve("vocabularies.db.aggregate-hash");
        Files.writeString(sidecar, "deadbeefcafe\n");

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);
        String etag = controller.serve(new MockHttpServletRequest()).getHeaders().getETag();

        assertThat(etag).isEqualTo("\"deadbeefcafe\"");
    }

    @Test
    void etagFollowsSidecarUpdates(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "v1".getBytes(StandardCharsets.UTF_8));
        Path sidecar = tempDir.resolve("vocabularies.db.aggregate-hash");
        Files.writeString(sidecar, "hash-v1");

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);
        String first = controller.serve(new MockHttpServletRequest()).getHeaders().getETag();

        Files.writeString(sidecar, "hash-v2");
        String second = controller.serve(new MockHttpServletRequest()).getHeaders().getETag();

        assertThat(first).isEqualTo("\"hash-v1\"");
        assertThat(second).isEqualTo("\"hash-v2\"");
    }

    @Test
    void etagFallsBackToFileSha256WhenSidecarMissing(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vocabularies.db");
        Files.write(file, "fake-sqlite".getBytes(StandardCharsets.UTF_8));

        VocabulariesDbController controller = new VocabulariesDbController(file.toString(), CACHE_MAX_AGE);
        String etag = controller.serve(new MockHttpServletRequest()).getHeaders().getETag();

        // Bare hex sha256 of "fake-sqlite", quoted by the controller.
        assertThat(etag)
                .startsWith("\"")
                .endsWith("\"")
                .matches("^\"[0-9a-f]{64}\"$");
    }
}
