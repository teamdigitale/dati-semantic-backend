package it.teamdigitale.ndc.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class VocabularyItemNotFoundException extends RuntimeException {
    public VocabularyItemNotFoundException(String index, String id) {
        super(String.format("Cannot find id '%s' in index '%s'", id, index));
    }
}
