package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.exception.UnknownTypeIriException;
import lombok.Getter;

@Getter
public enum SemanticAssetType {
    ONTOLOGY("ontology", "Ontologie", "http://www.w3.org/2002/07/owl#Ontology", true),
    CONTROLLED_VOCABULARY("controlled vocabulary", "VocabolariControllati",
            "http://dati.gov.it/onto/dcatapit#Dataset", false),
    SCHEMA("schema", null, "to be added", false);

    private final String description;
    private final String folderName;
    private final String typeIri;
    private final boolean isIgnoringObsoleteVersions;

    SemanticAssetType(String description, String folderName, String typeIri, boolean isIgnoringObsoleteVersions) {
        this.description = description;
        this.folderName = folderName;
        this.typeIri = typeIri;
        this.isIgnoringObsoleteVersions = isIgnoringObsoleteVersions;
    }

    public static SemanticAssetType getByIri(String typeIri) {
        for (SemanticAssetType type : SemanticAssetType.values()) {
            if (type.getTypeIri().equals(typeIri)) {
                return type;
            }
        }
        throw new UnknownTypeIriException(typeIri);
    }

    @Override
    public String toString() {
        return description;
    }
}
