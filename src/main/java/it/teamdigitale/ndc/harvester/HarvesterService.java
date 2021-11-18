package it.teamdigitale.ndc.harvester;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

@Component
public class HarvesterService {
    private final AgencyRepositoryService agencyRepositoryService;
    private final CsvParser csvParser;

    public HarvesterService(AgencyRepositoryService agencyRepositoryService, CsvParser csvParser) {

        this.agencyRepositoryService = agencyRepositoryService;
        this.csvParser = csvParser;
    }

    public void harvest(String repoUrl) throws GitAPIException, IOException {
        Path path = agencyRepositoryService.cloneRepo(repoUrl);
        agencyRepositoryService.getControlledVocabularyPaths(path);
    }
}
