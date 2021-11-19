package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.service.VocabularyDataService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

@Component
public class HarvesterService {
    private final AgencyRepositoryService agencyRepositoryService;
    private final CsvParser csvParser;
    private final SemanticAssetsParser semanticAssetsParser;
    private final VocabularyDataService vocabularyDataService;

    public HarvesterService(AgencyRepositoryService agencyRepositoryService, CsvParser csvParser,
                            SemanticAssetsParser semanticAssetsParser,
                            VocabularyDataService vocabularyDataService) {
        this.agencyRepositoryService = agencyRepositoryService;
        this.csvParser = csvParser;
        this.semanticAssetsParser = semanticAssetsParser;
        this.vocabularyDataService = vocabularyDataService;
    }

    public void harvest(String repoUrl) throws GitAPIException, IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl);
        List<CvPath> controlledVocabularyPaths =
            agencyRepositoryService.getControlledVocabularyPaths(path);
        for (CvPath cvPath : controlledVocabularyPaths) {
            Resource controlledVocabulary =
                semanticAssetsParser.getControlledVocabulary(cvPath.getTtlPath());
            String vocabularyId = semanticAssetsParser.getKeyConcept(controlledVocabulary);
            String rightsHolderId = semanticAssetsParser.getRightsHolderId(controlledVocabulary);
            List<Map<String, String>> flatData = csvParser.convertCsvToJson(cvPath.getCsvPath());
            vocabularyDataService.indexData(rightsHolderId, vocabularyId, flatData);
        }
    }
}
