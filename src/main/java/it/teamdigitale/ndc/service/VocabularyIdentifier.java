package it.teamdigitale.ndc.service;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class VocabularyIdentifier {
    private final String rightsHolder;
    private final String keyConcept;

    private final String indexName;

    public VocabularyIdentifier(String rightsHolder, String keyConcept) {
        this.rightsHolder = rightsHolder;
        this.keyConcept = keyConcept;
        this.indexName = String.join(".", rightsHolder, keyConcept).toLowerCase();
    }

    public String getIndexName() {
        return indexName;
    }

    @Override
    public String toString() {
        return indexName;
    }
}
