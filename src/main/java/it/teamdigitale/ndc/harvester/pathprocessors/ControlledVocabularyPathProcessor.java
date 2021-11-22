package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.service.VocabularyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ControlledVocabularyPathProcessor {
    private final SemanticAssetModelFactory modelFactory;
    private final CsvParser csvParser;
    private final VocabularyDataService vocabularyDataService;

    public void process(CvPath path) {
        ControlledVocabularyModel model = modelFactory.createControlledVocabulary(path.getTtlPath());
        String vocabularyId = model.getKeyConcept();
        String rightsHolder = model.getRightsHolderId();

        path.getCsvPath().ifPresent(p -> parseAndIndexCsv(vocabularyId, rightsHolder, p));

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }

    private void parseAndIndexCsv(String vocabularyId, String rightsHolder, String csvPath) {
        List<Map<String, String>> flatData = csvParser.convertCsvToJson(csvPath);
        vocabularyDataService.indexData(rightsHolder, vocabularyId, flatData);
    }
}
