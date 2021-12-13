package it.teamdigitale.ndc.harvester;

import lombok.Getter;

@Getter
public enum SemanticAssetType {
    ONTOLOGY("ontology",
            "Ontologie",
            "http://www.w3.org/2002/07/owl#Ontology",
            true),
    CONTROLLED_VOCABULARY("controlled vocabulary",
            "VocabolariControllati",
            "http://dati.gov.it/onto/dcatapit#Dataset",
            false),
    SCHEMA("schema",
            "Schema",
            "http://dati.gov.it/onto/dcatapit#Dataset",
            false);

    private final String description;
    private final String folderName;
    private final String typeIri;
    private final boolean isIgnoringObsoleteVersions;

    SemanticAssetType(String description, String folderName, String typeIri,
                      boolean isIgnoringObsoleteVersions) {
        this.description = description;
        this.folderName = folderName;
        this.typeIri = typeIri;
        this.isIgnoringObsoleteVersions = isIgnoringObsoleteVersions;
    }

    @Override
    public String toString() {
        return description;
    }
}
