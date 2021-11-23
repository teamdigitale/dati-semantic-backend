package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.service.VocabularyDataService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ControlledVocabularyPathProcessor extends SemanticAssetPathProcessor<CvPath, ControlledVocabularyModel> {
    private final SemanticAssetModelFactory modelFactory;
    private final CsvParser csvParser;
    private final VocabularyDataService vocabularyDataService;

    public ControlledVocabularyPathProcessor(TripleStoreRepository repository, SemanticAssetModelFactory modelFactory, CsvParser csvParser, VocabularyDataService vocabularyDataService) {
        super(repository);
        this.modelFactory = modelFactory;
        this.csvParser = csvParser;
        this.vocabularyDataService = vocabularyDataService;
    }

    @Override
    protected void processWithModel(String repoUrl, CvPath path, ControlledVocabularyModel model) {
        super.processWithModel(repoUrl, path, model);

        String vocabularyId = model.getKeyConcept();
        String rightsHolder = model.getRightsHolderId();

        path.getCsvPath().ifPresent(p -> parseAndIndexCsv(vocabularyId, rightsHolder, p));
    }

    @Override
    protected ControlledVocabularyModel loadModel(String ttlFile) {
        return modelFactory.createControlledVocabulary(ttlFile);
    }

    private void parseAndIndexCsv(String vocabularyId, String rightsHolder, String csvPath) {
        List<Map<String, String>> flatData = csvParser.convertCsvToMapList(csvPath);
        vocabularyDataService.indexData(rightsHolder, vocabularyId, flatData);
    }
}
