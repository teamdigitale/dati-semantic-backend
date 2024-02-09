package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.model.CvPath;
import it.gov.innovazione.ndc.harvester.pathprocessors.ControlledVocabularyPathProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyHarvesterTest {
    @Mock
    AgencyRepositoryService agencyRepositoryService;
    @Mock
    ControlledVocabularyPathProcessor pathProcessor;
    @InjectMocks
    ControlledVocabularyHarvester harvester;

    @Test
    void shouldProcessAllScannedPaths() {
        final Path cvBasePath = Path.of("assets/ontologies");
        final String repoUrl = "my-repo.git";
        final CvPath path1 = CvPath.of("onto1.ttl", "onto1.csv");
        final CvPath path2 = CvPath.of("onto2.ttl", "onto2.csv");
        when(agencyRepositoryService.getControlledVocabularyPaths(cvBasePath)).thenReturn(List.of(path1, path2));

        harvester.harvest(asRepo(repoUrl), cvBasePath);

        verify(agencyRepositoryService).getControlledVocabularyPaths(cvBasePath);
        verify(pathProcessor).process(repoUrl, path1);
        verify(pathProcessor).process(repoUrl, path2);
    }

    @Test
    void shouldCleanIndicesBeforeHarvesting() {
        String repoUrl = "my-repo.git";

        harvester.cleanUpBeforeHarvesting(repoUrl);

        verify(pathProcessor).dropCsvIndicesForRepo(repoUrl);
    }

    @Test
    void shouldReturnAllFiles() {
        final CvPath path = CvPath.of("onto1.ttl", "onto1.csv");
        List<File> allFiles = path.getAllFiles();

        assertEquals("onto1.ttl", allFiles.get(0).getName());
        assertEquals("onto1.csv", allFiles.get(1).getName());
    }

    @Test
    void shouldReturnTTLOnly() {
        final CvPath path = CvPath.of("onto1.ttl", null);
        List<File> allFiles = path.getAllFiles();

        assertEquals("onto1.ttl", allFiles.get(0).getName());
        assertEquals(1, allFiles.size());
    }
}
