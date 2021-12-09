package it.teamdigitale.ndc.service;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class VocabularyIdentifier {
    private final String agencyId;
    private final String keyConcept;

    private final String indexName;

    public VocabularyIdentifier(String agencyId, String keyConcept) {
        this.agencyId = agencyId;
        this.keyConcept = keyConcept;
        this.indexName = String.join(".", agencyId, keyConcept).toLowerCase();
    }

    public String getIndexName() {
        return indexName;
    }

    @Override
    public String toString() {
        return indexName;
    }
}
