package it.teamdigitale.ndc.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class VocabularyDataNotFoundException extends RuntimeException {
    public VocabularyDataNotFoundException(String index) {
        super("Unable to find vocabulary data for : " + index);
    }
}
