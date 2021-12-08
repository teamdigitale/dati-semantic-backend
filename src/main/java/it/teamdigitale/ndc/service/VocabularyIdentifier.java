package it.teamdigitale.ndc.service;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class VocabularyIdentifier {
    private final String rightsHolder;
    private final String keyConcept;

    public String getIndexName() {
        return String.join(".", rightsHolder, keyConcept).toLowerCase();
    }

    @Override
    public String toString() {
        return getIndexName();
    }
}
