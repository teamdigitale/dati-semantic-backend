package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.service.VocabularyDataService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HarvesterService {
    private final AgencyRepositoryService agencyRepositoryService;
    private final CsvParser csvParser;
    private final SemanticAssetsParser semanticAssetsParser;
    private final VocabularyDataService vocabularyDataService;

    public HarvesterService(
            AgencyRepositoryService agencyRepositoryService,
            CsvParser csvParser,
            SemanticAssetsParser semanticAssetsParser,
            VocabularyDataService vocabularyDataService) {
        this.agencyRepositoryService = agencyRepositoryService;
        this.csvParser = csvParser;
        this.semanticAssetsParser = semanticAssetsParser;
        this.vocabularyDataService = vocabularyDataService;
    }

    public void harvest(String repoUrl) throws GitAPIException, IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl);
        harvestControlledVocabularies(path);
        harvestOntologies(path);
    }

    private void harvestOntologies(Path path) {
        List<SemanticAssetPath> ontologyPaths = agencyRepositoryService.getOntologyPaths(path);
        for (SemanticAssetPath ontologyPath : ontologyPaths) {
            processOntologyPath(ontologyPath);
        }
    }

    private void processOntologyPath(SemanticAssetPath ontologyPath) {
        log.info("parsing and loading file {}", ontologyPath.getTtlPath());
        // load the ttl file into memory

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }

    private void harvestControlledVocabularies(Path path) {
        List<CvPath> controlledVocabularyPaths =
                agencyRepositoryService.getControlledVocabularyPaths(path);
        for (CvPath cvPath : controlledVocabularyPaths) {
            processControlledVocabularyPath(cvPath);
        }
    }

    private void processControlledVocabularyPath(CvPath cvPath) {
        Resource controlledVocabulary =
                semanticAssetsParser.getControlledVocabulary(cvPath.getTtlPath());
        String vocabularyId = semanticAssetsParser.getKeyConcept(controlledVocabulary);
        String rightsHolderId = semanticAssetsParser.getRightsHolderId(controlledVocabulary);

        cvPath.getCsvPath().ifPresent(p -> parseAndIndexCsv(vocabularyId, rightsHolderId, p));

        // store the metadata into Elasticsearch main index
        // store the resource into Virtuoso
    }

    private void parseAndIndexCsv(String vocabularyId, String rightsHolderId, String csvPath) {
        List<Map<String, String>> flatData = csvParser.convertCsvToJson(csvPath);
        vocabularyDataService.indexData(rightsHolderId, vocabularyId, flatData);
    }
}
