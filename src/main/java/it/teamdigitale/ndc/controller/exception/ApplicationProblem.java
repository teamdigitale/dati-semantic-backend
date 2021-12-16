package it.teamdigitale.ndc.controller.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationProblem {
    private final String type;
    private final String title;
    private final int status;
    private final String timestamp;

    @Builder
    public ApplicationProblem(String type, String title, int status) {
        this.type = type;
        this.title = title;
        this.status = status;
        timestamp = LocalDateTime.now().toString();
    }

    public static String getErrorUri(String errorName) {
        return "http://schema.gov.it/tech/errors/" + errorName;
    }
}
