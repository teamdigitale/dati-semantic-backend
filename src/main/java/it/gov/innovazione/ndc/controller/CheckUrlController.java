package it.gov.innovazione.ndc.controller;

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
    @GetMapping
    @SneakyThrows
    public ResponseEntity<?> check(@RequestParam String url) {
        log.info("Checking url {}", url);
        HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();
        huc.setConnectTimeout(5000);
        log.info("Response code for url {} is : {}", url, huc.getResponseCode());
        return new ResponseEntity<>(HttpStatus.valueOf(huc.getResponseCode()));
    }
}
