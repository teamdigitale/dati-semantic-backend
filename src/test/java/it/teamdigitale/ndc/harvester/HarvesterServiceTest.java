package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.service.VocabularyDataService;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HarvesterServiceTest {
    @Mock
    AgencyRepositoryService agencyRepoService;
    @Mock
    CsvParser csvParser;
    @Mock
    SemanticAssetsParser semanticAssetsParser;
    @Mock
    Resource controlledVocabulary;
    @Mock
    VocabularyDataService vocabularyDataService;
    @InjectMocks
    HarvesterService harvester;

    @Test
    void shouldHarvestCsvFiles() throws GitAPIException, IOException {
        String repoUrl = "someRepoUri";
        File clonedRepo = new File("/tmp/ndc-1234");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepo.toPath());
        when(agencyRepoService.getControlledVocabularyPaths(clonedRepo.toPath()))
                .thenReturn(List.of(CvPath.of("test.ttl", "test.csv"), CvPath.of("test.ttl", "test.csv")));
        when(semanticAssetsParser.getControlledVocabulary("test.ttl")).thenReturn(controlledVocabulary);
        when(semanticAssetsParser.getKeyConcept(controlledVocabulary)).thenReturn("keyConcept");
        when(semanticAssetsParser.getRightsHolderId(controlledVocabulary)).thenReturn("rightsHolderId");
        when(csvParser.convertCsvToJson("test.csv")).thenReturn(List.of(Map.of("key", "val")));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getControlledVocabularyPaths(Path.of("/tmp/ndc-1234"));
        verify(csvParser, times(2)).convertCsvToJson("test.csv");
        verify(semanticAssetsParser, times(2)).getControlledVocabulary("test.ttl");
        verify(semanticAssetsParser, times(2)).getKeyConcept(controlledVocabulary);
        verify(semanticAssetsParser, times(2)).getRightsHolderId(controlledVocabulary);
        verify(vocabularyDataService, times(2))
                .indexData("rightsHolderId", "keyConcept", List.of(Map.of("key", "val")));
    }

    @Test
    void shouldHarvestOntologyFiles() throws GitAPIException, IOException {
        String repoUrl = "someRepoUri";
        File clonedRepo = new File("/tmp/ndc-1234");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepo.toPath());
        when(agencyRepoService.getOntologyPaths(clonedRepo.toPath()))
                .thenReturn(List.of(new SemanticAssetPath("test.ttl"), new SemanticAssetPath("test.ttl")));

        harvester.harvest(repoUrl);

        verify(agencyRepoService, times(1)).cloneRepo("someRepoUri");
        verify(agencyRepoService).getOntologyPaths(Path.of("/tmp/ndc-1234"));
    }
}
