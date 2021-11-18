package it.teamdigitale.ndc.harvester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.teamdigitale.ndc.harvester.model.CvPath;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

public class HarvesterServiceTest {


    @Test
    void shouldHarvestCsvFiles() throws GitAPIException, IOException {
        String repoUrl = "someRepoUri";
        File clonedRepo = new File("/tmp/ndc-1234");
        AgencyRepositoryService agencyRepoService = mock(AgencyRepositoryService.class);
        CsvParser csvParser = mock(CsvParser.class);
        HarvesterService harvester = new HarvesterService(agencyRepoService, csvParser);
        when(agencyRepoService.cloneRepo(repoUrl)).thenReturn(clonedRepo.toPath());
        when(agencyRepoService.getControlledVocabularyPaths(clonedRepo.toPath()))
            .thenReturn(List.of(CvPath.of("test.csv", "test.ttl")));

        harvester.harvest(repoUrl);

        verify(agencyRepoService).cloneRepo("someRepoUri");
        verify(agencyRepoService).getControlledVocabularyPaths(Path.of("/tmp/ndc-1234"));
    }
}
