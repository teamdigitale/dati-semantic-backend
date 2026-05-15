package it.gov.innovazione.ndc.service.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;

import java.util.Optional;

public interface AssetDeltaClassifier {

    boolean supports(SemanticAssetType type);

    /**
     * Classifies added/removed triples into a structured summary JSON for a single asset.
     *
     * @param assetIri    IRI of the asset being classified
     * @param added       triples present in TMP but not in ONLINE
     * @param removed     triples present in ONLINE but not in TMP
     * @param tmpModel    full snapshot of the asset triples in TMP (for type lookups)
     * @param onlineModel full snapshot of the asset triples in ONLINE (for type lookups)
     * @return summary JSON for the MODIFIED row, or empty if both inputs are empty
     *         (in which case the asset is considered UNCHANGED and no row should be written)
     */
    Optional<String> classifyModified(String assetIri, Model added, Model removed,
                                       Model tmpModel, Model onlineModel);

    /**
     * Synthetic summary for a newly added asset. Counts top-level elements.
     */
    String summarizeAdded(String assetIri, Model assetModel);

    /**
     * Optional snapshot summary for a removed asset. Default: null (no summary).
     */
    default String summarizeRemoved(String assetIri, Model lastKnownModel) {
        return null;
    }
}
