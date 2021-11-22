package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import it.teamdigitale.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import it.teamdigitale.ndc.harvester.pathprocessors.OntologyPathProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HarvesterServiceTest {
    @Mock
    AgencyRepositoryService agencyRepoService;
    @Mock
    ControlledVocabularyPathProcessor controlledVocabularyPathProcessor;
    @Mock
    OntologyPathProcessor ontologyPathProcessor;

    @InjectMocks
    HarvesterService harvester;

    @Test
    void shouldHarvestControlledVocabularies() throws IOException {
        String repoUrl = "someRepoUri";
        File clonedRepo = new File("/tmp/ndc-1234");
        CvPath path1 = CvPath.of("test1.ttl", "test1.csv");
        CvPath path2 = CvPath.of("test2.ttl", "test2.csv");
        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepo.toPath());
        when(agencyRepoService.getControlledVocabularyPaths(clonedRepo.toPath())).thenReturn(List.of(path1, path2));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getControlledVocabularyPaths(Path.of("/tmp/ndc-1234"));
        verify(controlledVocabularyPathProcessor).process(path1);
        verify(controlledVocabularyPathProcessor).process(path2);
    }

    @Test
    void shouldHarvestOntologyFiles() throws IOException {
        String repoUrl = "someRepoUri";
        File clonedRepo = new File("/tmp/ndc-1234");
        SemanticAssetPath path1 = new SemanticAssetPath("test1.ttl");
        SemanticAssetPath path2 = new SemanticAssetPath("test2.ttl");

        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepo.toPath());
        when(agencyRepoService.getOntologyPaths(clonedRepo.toPath())).thenReturn(List.of(path1, path2));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getOntologyPaths(Path.of("/tmp/ndc-1234"));
        verify(ontologyPathProcessor).process(path1);
        verify(ontologyPathProcessor).process(path2);
    }
}
