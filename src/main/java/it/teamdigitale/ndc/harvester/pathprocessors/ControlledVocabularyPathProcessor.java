package it.teamdigitale.ndc.harvester.pathprocessors;

import it.teamdigitale.ndc.harvester.CsvParser;
import it.teamdigitale.ndc.harvester.model.ControlledVocabularyModel;
import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetModelFactory;
import it.teamdigitale.ndc.repository.SemanticAssetMetadataRepository;
import it.teamdigitale.ndc.repository.TripleStoreRepository;
import it.teamdigitale.ndc.service.VocabularyDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ControlledVocabularyPathProcessor extends SemanticAssetPathProcessor<CvPath, ControlledVocabularyModel> {
    private final SemanticAssetModelFactory modelFactory;
    private final CsvParser csvParser;
    private final VocabularyDataService vocabularyDataService;
    private final String baseUrl;

    public ControlledVocabularyPathProcessor(TripleStoreRepository tripleStoreRepository, SemanticAssetModelFactory modelFactory,
                                             CsvParser csvParser, VocabularyDataService vocabularyDataService,
                                             SemanticAssetMetadataRepository metadataRepository,
                                             @Value("${ndc.baseUrl}") String baseUrl) {
        super(tripleStoreRepository, metadataRepository);
        this.modelFactory = modelFactory;
        this.csvParser = csvParser;
        this.vocabularyDataService = vocabularyDataService;
        this.baseUrl = baseUrl;
    }

    @Override
    protected void processWithModel(String repoUrl, CvPath path, ControlledVocabularyModel model) {
        super.processWithModel(repoUrl, path, model);

        path.getCsvPath().ifPresent(p -> {
            String vocabularyId = model.getKeyConcept();
            String rightsHolder = model.getRightsHolderId();

            parseAndIndexCsv(vocabularyId, rightsHolder, p);
        });
    }

    @Override
    protected ControlledVocabularyModel loadModel(String ttlFile) {
        return modelFactory.createControlledVocabulary(ttlFile);
    }

    @Override
    protected void enrichModelBeforePersisting(ControlledVocabularyModel model, CvPath path) {
        path.getCsvPath().ifPresent(p -> model.addNdcUrlProperty(baseUrl));
    }

    private void parseAndIndexCsv(String vocabularyId, String rightsHolder, String csvPath) {
        List<Map<String, String>> flatData = csvParser.convertCsvToMapList(csvPath);
        vocabularyDataService.indexData(rightsHolder, vocabularyId, flatData);
    }
}
