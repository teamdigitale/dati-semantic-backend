package it.teamdigitale.ndc.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class AppJsonResponse {
    private AppJsonResponse() {
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
