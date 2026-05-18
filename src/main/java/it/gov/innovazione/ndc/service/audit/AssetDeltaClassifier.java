package it.gov.innovazione.ndc.service.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import org.apache.jena.rdf.model.Model;

import java.util.Optional;

public interface AssetDeltaClassifier {

    boolean supports(SemanticAssetType type);

    /**
     * Builds a structured summary JSON for an asset, in a shape that is identical
     * across ADDED / REMOVED / MODIFIED change kinds.
     *
     * <p>The caller chooses what to pass as {@code added}/{@code removed} based on the change kind:
     * <ul>
     *   <li>ADDED: {@code added} = full asset model from TMP, {@code removed} = empty model</li>
     *   <li>REMOVED: {@code added} = empty model, {@code removed} = full asset model from ONLINE</li>
     *   <li>MODIFIED: {@code added} = tmpModel \ onlineModel, {@code removed} = onlineModel \ tmpModel</li>
     * </ul>
     *
     * @param assetIri    IRI of the asset being classified
     * @param added       triples to attribute to the "added" side
     * @param removed     triples to attribute to the "removed" side
     * @param tmpModel    full snapshot of the asset in TMP (empty for REMOVED)
     * @param onlineModel full snapshot of the asset in ONLINE (empty for ADDED)
     * @return summary JSON, or empty if both {@code added} and {@code removed} are empty
     *         (in which case the asset is considered UNCHANGED and no row should be written).
     */
    Optional<String> classify(String assetIri, Model added, Model removed,
                               Model tmpModel, Model onlineModel);
}
