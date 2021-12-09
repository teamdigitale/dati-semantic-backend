package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.CvPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Disabled("This test is flaky, as it depends on the status of an external git repo! Only use by hand")
public class ScansIntegrationTest {
    @Autowired
    private AgencyRepositoryService agencyRepositoryService;

    @Test
    void serviceExists() throws IOException {
        assertThat(agencyRepositoryService).isNotNull();

        List<CvPath> cvPaths;
        Path repoPath = agencyRepositoryService.cloneRepo("https://github.com/italia/daf-ontologie-vocabolari-controllati.git");
        try {
            cvPaths = agencyRepositoryService.getControlledVocabularyPaths(repoPath);
        } finally {
            agencyRepositoryService.removeClonedRepo(repoPath);
        }

        assertThat(cvPaths.size()).isEqualTo(36);
    }
}
