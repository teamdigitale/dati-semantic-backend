package it.teamdigitale.ndc.harvester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.service.VocabularyDataService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

public class HarvesterServiceTest {


    @Test
    void shouldHarvestCsvFiles() throws GitAPIException, IOException {
        String repoUrl = "someRepoUri";
        File clonedRepo = new File("/tmp/ndc-1234");
        AgencyRepositoryService agencyRepoService = mock(AgencyRepositoryService.class);
        CsvParser csvParser = mock(CsvParser.class);
        SemanticAssetsParser semanticAssetsParser = mock(SemanticAssetsParser.class);
        Resource controlledVocabulary = mock(Resource.class);
        VocabularyDataService vocabularyDataService = mock(VocabularyDataService.class);
        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepo.toPath());
        when(agencyRepoService.getControlledVocabularyPaths(clonedRepo.toPath()))
            .thenReturn(
                List.of(CvPath.of("test.csv", "test.ttl"), CvPath.of("test.csv", "test.ttl")));
        when(semanticAssetsParser.getControlledVocabulary("test.ttl"))
            .thenReturn(controlledVocabulary);
        when(semanticAssetsParser.getKeyConcept(controlledVocabulary)).thenReturn("keyConcept");
        when(semanticAssetsParser.getRightsHolderId(controlledVocabulary))
            .thenReturn("rightsHolderId");
        when(csvParser.convertCsvToJson("test.csv")).thenReturn(List.of(Map.of("key", "val")));
        HarvesterService harvester = new HarvesterService(agencyRepoService, csvParser,
            semanticAssetsParser, vocabularyDataService);

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getControlledVocabularyPaths(Path.of("/tmp/ndc-1234"));
        verify(csvParser, times(2)).convertCsvToJson("test.csv");
        verify(semanticAssetsParser, times(2)).getControlledVocabulary("test.ttl");
        verify(semanticAssetsParser, times(2)).getKeyConcept(controlledVocabulary);
        verify(semanticAssetsParser, times(2)).getRightsHolderId(controlledVocabulary);
        verify(vocabularyDataService, times(2)).indexData("rightsHolderId", "keyConcept",
            List.of(Map.of("key", "val")));
    }
}
