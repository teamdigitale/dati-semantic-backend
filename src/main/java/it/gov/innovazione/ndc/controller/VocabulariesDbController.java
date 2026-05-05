package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.csvapis.Sha256Hasher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Serves the aggregated {@code vocabularies.db} as a static asset, with
 * cache-friendly headers compatible with the upstream {@code apiv1} entrypoint
 * (https-only, no redirect, single 200 response, &le; 100 MB).
 */
@RestController
@RequestMapping("/harvest")
@Slf4j
public class VocabulariesDbController {

    private static final MediaType SQLITE_MEDIA_TYPE = MediaType.parseMediaType("application/vnd.sqlite3");

    private final String aggregateDbPath;
    private final long cacheMaxAgeSeconds;

    public VocabulariesDbController(
            @Value("${harvester.csvapis.aggregate-db.path}") String aggregateDbPath,
            @Value("${harvester.csvapis.aggregate-db.cache-max-age-seconds:86400}") long cacheMaxAgeSeconds) {
        this.aggregateDbPath = aggregateDbPath;
        this.cacheMaxAgeSeconds = cacheMaxAgeSeconds;
    }

    @GetMapping("/vocabularies.db")
    public ResponseEntity<Resource> serve(HttpServletRequest request) throws IOException {
        Path path = Paths.get(aggregateDbPath);
        if (!Files.isRegularFile(path)) {
            log.debug("Aggregate db not yet available at {}", path);
            return ResponseEntity.notFound().build();
        }

        long lastModifiedMillis = Files.getLastModifiedTime(path).toMillis();
        String etag = "\"" + computeEtag(path) + "\"";

        if (matchesIfNoneMatch(request, etag) || isNotModifiedSince(request, lastModifiedMillis)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .lastModified(lastModifiedMillis)
                    .cacheControl(cacheControl())
                    .build();
        }

        Resource body = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(SQLITE_MEDIA_TYPE)
                .contentLength(Files.size(path))
                .eTag(etag)
                .lastModified(lastModifiedMillis)
                .cacheControl(cacheControl())
                .body(body);
    }

    /**
     * Read the precomputed aggregate hash from the sidecar file written by
     * {@code VocabulariesDbAggregationService}. Falls back to hashing the
     * aggregate file when the sidecar is missing (e.g. file dropped in place
     * manually or upgrade path), so the endpoint stays usable.
     */
    private String computeEtag(Path aggregateDb) throws IOException {
        Path sidecar = Paths.get(aggregateDb.toString() + ".aggregate-hash");
        if (Files.isRegularFile(sidecar)) {
            String content = Files.readString(sidecar).trim();
            if (!content.isEmpty()) {
                return content;
            }
        }
        log.debug("Aggregate-hash sidecar missing or empty at {}; falling back to file digest", sidecar);
        return Sha256Hasher.hashFile(aggregateDb);
    }

    private CacheControl cacheControl() {
        return CacheControl.maxAge(Duration.ofSeconds(cacheMaxAgeSeconds)).cachePublic();
    }

    private boolean matchesIfNoneMatch(HttpServletRequest request, String etag) {
        String header = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (header == null) {
            return false;
        }
        for (String token : header.split(",")) {
            if (etag.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNotModifiedSince(HttpServletRequest request, long lastModifiedMillis) {
        long ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince < 0) {
            return false;
        }
        // HTTP-date precision is seconds.
        return lastModifiedMillis / 1000 <= ifModifiedSince / 1000;
    }
}
