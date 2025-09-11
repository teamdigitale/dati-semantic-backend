package it.gov.innovazione.ndc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RestController
@RequestMapping("check-url")
@Slf4j
public class CheckUrlController {

    public static final String ACCEPTED_MIME_TYPES =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,"
            + "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";

    // User-Agent “normale” per evitare 403 da alcuni server (es. w3id)
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/122.0.0.0 Safari/537.36";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)   // segue TUTTI i redirect
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @GetMapping
    @Operation(
            operationId = "checkUrl",
            description = "Check if the passed URL is available",
            summary = "Check the URL status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The URL is available",
                            content = @Content(schema = @Schema(implementation = Integer.class))),
                    @ApiResponse(responseCode = "404", description = "The URL is not available",
                            content = @Content(schema = @Schema(implementation = Integer.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected error",
                            content = @Content(schema = @Schema(implementation = Integer.class)))
            }
    )
    public ResponseEntity<Void> check(@RequestParam String url) {
        try {
            log.info("Checking url {}", url);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", ACCEPTED_MIME_TYPES)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9,it-IT;q=0.8")
                    .GET() // usa GET: alcuni host rispondono 403/405 a HEAD
                    .build();

            // Non scaricare il body (più veloce, meno banda)
            HttpResponse<Void> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.discarding()
            );

            int status = response.statusCode();
            log.info("Final response code for {} -> {}", url, status);

            return ResponseEntity.status(status).build();

        } catch (IllegalArgumentException e) {
            // URL malformata
            log.warn("Invalid URL: {}", url, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error checking URL {}", url, e);
            return ResponseEntity.status(500).build();
        }
    }
}
