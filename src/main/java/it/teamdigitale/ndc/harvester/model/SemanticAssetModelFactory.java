package it.teamdigitale.ndc.harvester.model;

import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SemanticAssetModelFactory {
    public ControlledVocabularyModel createControlledVocabulary(String ttlFile) {
        Model model = RDFDataMgr.loadModel(ttlFile, Lang.TURTLE);
        return new ControlledVocabularyModel(model, ttlFile);
    }
}
