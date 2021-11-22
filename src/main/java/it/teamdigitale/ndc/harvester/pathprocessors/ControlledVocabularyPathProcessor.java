package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.service.VocabularyDataService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ControlledVocabularyPathProcessor {
    private final SemanticAssetsParser semanticAssetsParser;
    private final CsvParser csvParser;
    private final VocabularyDataService vocabularyDataService;

    public void process(CvPath path) {
        Resource controlledVocabulary =
            semanticAssetsParser.getControlledVocabulary(path.getTtlPath());
        String vocabularyId = semanticAssetsParser.getKeyConcept(controlledVocabulary);
        String rightsHolder = semanticAssetsParser.getRightsHolderId(controlledVocabulary);

        path.getCsvPath().ifPresent(p -> parseAndIndexCsv(vocabularyId, rightsHolder, p));

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }

    private void parseAndIndexCsv(String vocabularyId, String rightsHolder, String csvPath) {
        List<Map<String, String>> flatData = csvParser.convertCsvToJson(csvPath);
        vocabularyDataService.indexData(rightsHolder, vocabularyId, flatData);
    }
}
