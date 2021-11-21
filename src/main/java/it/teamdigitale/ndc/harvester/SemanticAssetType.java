package it.teamdigitale.ndc.harvester;

import lombok.Getter;

@Getter
public enum SemanticAssetType {
    ONTOLOGY("ontology", "Ontologie"),
    CONTROLLED_VOCABULARY("controlled vocabulary", "VocabolariControllati"),
    SCHEMA("schema", null);

    private final String description;
    private final String folderName;

    SemanticAssetType(String description, String folderName) {
        this.description = description;
        this.folderName = folderName;
    }
}
