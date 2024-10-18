package it.gov.innovazione.ndc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("check-url")
@Slf4j
public class CheckUrlController {

    public static final String ACCEPTED_MIME_TYPES =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,"
            + "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";

    @GetMapping
    @SneakyThrows
    @Operation(
            operationId = "checkUrl",
            description = "Check if the passed URL is available",
            summary = "Check the URL status",
            responses = {
                @ApiResponse(responseCode = "200", description = "The URL is available",
                            content = @Content(schema = @Schema(implementation = HttpStatus.class))),
                @ApiResponse(responseCode = "404", description = "The URL is not available",
                            content = @Content(schema = @Schema(implementation = HttpStatus.class))),
                @ApiResponse(responseCode = "500", description = "The URL is not available",
                            content = @Content(schema = @Schema(implementation = HttpStatus.class)))})
    public ResponseEntity<?> check(@RequestParam String url) {
        log.info("Checking url {}", url);
        HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();
        huc.setConnectTimeout(5000);
        huc.setRequestProperty("Accept", ACCEPTED_MIME_TYPES);
        log.info("Response code for url {} is : {}", url, huc.getResponseCode());
        return new ResponseEntity<>(HttpStatus.valueOf(huc.getResponseCode()));
    }
}
