package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.service.VocabularyDataService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ControlledVocabularyPathProcessor extends SemanticAssetPathProcessor<CvPath> {
    private final CsvParser csvParser;
    private final VocabularyDataService vocabularyDataService;

    public ControlledVocabularyPathProcessor(SemanticAssetModelFactory modelFactory, CsvParser csvParser, VocabularyDataService vocabularyDataService) {
        super(modelFactory);
        this.csvParser = csvParser;
        this.vocabularyDataService = vocabularyDataService;
    }

    @Override
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
