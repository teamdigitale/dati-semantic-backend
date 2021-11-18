package it.teamdigitale.ndc.harvester;

import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class HarvesterJob {

    private final HarvesterService harvesterService;

    private final List<String> repos;

    @Autowired
    public HarvesterJob(HarvesterService harvesterService,
                        @Value("#{'${harvester.repositories}'.split(',')}") List<String> repos) {
        this.harvesterService = harvesterService;
        this.repos = repos;
    }

    @Scheduled(cron = "0 0 22 ? * *")
    public void harvest() throws GitAPIException, IOException {
        for (String repo : repos) {
            harvesterService.harvest(repo);
        }
    }
}
