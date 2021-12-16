package it.teamdigitale.ndc.controller.exception;

public class VocabularyItemNotFoundException extends BaseNotFoundException {
    public VocabularyItemNotFoundException(String index, String id) {
        super(String.format("Cannot find id '%s' in index '%s'", id, index));
    }
}
