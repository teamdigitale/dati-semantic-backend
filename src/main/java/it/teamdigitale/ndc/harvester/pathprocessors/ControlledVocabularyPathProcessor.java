package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.harvester.SemanticAssetsParser;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.service.VocabularyDataService;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
        String rightsHolderId = semanticAssetsParser.getRightsHolderId(controlledVocabulary);

        path.getCsvPath().ifPresent(p -> parseAndIndexCsv(vocabularyId, rightsHolderId, p));

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }

    private void parseAndIndexCsv(String vocabularyId, String rightsHolderId, String csvPath) {
        List<Map<String, String>> flatData = csvParser.convertCsvToJson(csvPath);
        vocabularyDataService.indexData(rightsHolderId, vocabularyId, flatData);
    }
}
