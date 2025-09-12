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

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("check-url")
@Slf4j
public class CheckUrlController {

    private static final String ACCEPTED_MIME_TYPES =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,"
                    + "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";

    // User-Agent “normale” per evitare filtri anti-bot (CDN/WAF)
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Safari/537.36";

    // Cookie store per mantenere eventuali cookie lungo i redirect / challenge
    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

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

            // 1º tentativo: HTTP/2
            HttpResponse<Void> resp = send(url, HttpClient.Version.HTTP_2);
            logChain(resp);

            // Se 403 (tipico di CDN/WAF), riprova una volta in HTTP/1.1
            if (resp.statusCode() == 403) {
                log.info("Got 403 on HTTP/2. Retrying with HTTP/1.1 …");
                resp = send(url, HttpClient.Version.HTTP_1_1);
                logChain(resp);
            }

            int status = resp.statusCode();
            log.info("Final response code for {} -> {}", url, status);
            return ResponseEntity.status(status).build();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid URL: {}", url, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error checking URL {}", url, e);
            return ResponseEntity.status(500).build();
        }
    }

    /* ----------------------------- helpers ----------------------------- */

    private HttpResponse<Void> send(String url, HttpClient.Version version) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(version)
                .followRedirects(HttpClient.Redirect.NORMAL) // 301/302/303/307/308
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(8))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", ACCEPTED_MIME_TYPES)
                .header("Accept-Language", "en-US,en;q=0.9,it-IT;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                // Client Hints + Fetch metadata
                .header("sec-ch-ua", "\"Chromium\";v=\"124\", \"Not(A:Brand\";v=\"24\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Linux\"")
                .header("Sec-Fetch-Site", "cross-site")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-User", "?1")
                .header("Sec-Fetch-Dest", "document")
                .header("Referer", "https://w3id.org/italia/")
                .GET() // GET: alcuni host rifiutano HEAD (403/405)
                .build();

        // Non scarichiamo il body: ci basta lo status finale
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    /**
     * Logga la catena degli hop dalla prima richiesta all’ultima.
     */
    private void logChain(HttpResponse<?> lastResponse) {
        List<HttpResponse<?>> chain = new ArrayList<>();
        Optional<? extends HttpResponse<?>> cur = Optional.of(lastResponse);
        while (cur.isPresent()) {
            chain.add(cur.get());
            cur = cur.get().previousResponse();
        }
        Collections.reverse(chain); // dalla prima richiesta all’ultima

        for (int i = 0; i < chain.size(); i++) {
            HttpResponse<?> r = chain.get(i);
            String location = header(r, "Location");
            String server = header(r, "Server");
            String via = header(r, "Via");
            String cfRay = header(r, "CF-RAY");
            log.info("Hop {} -> {} | status={} | Location={} | Server={} | Via={} | CF-RAY={}",
                    i + 1, r.uri(), r.statusCode(), location, server, via, cfRay);
        }
    }

    private static String header(HttpResponse<?> r, String name) {
        return r.headers().firstValue(name).orElse("-");
    }
}
