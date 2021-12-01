package it.teamdigitale.ndc.harvester;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
//@EnableScheduling
@Slf4j
public class HarvesterJob {

    private final HarvesterService harvesterService;

    private final List<String> repos;

    @Autowired
    public HarvesterJob(HarvesterService harvesterService,
                        @Value("#{'${harvester.repositories}'.split(',')}") List<String> repos) {
        this.harvesterService = harvesterService;
        this.repos = repos;
    }

    //    @Scheduled(cron = "0 0 22 ? * *")
    public void harvest() {
        for (String repo : repos) {
            try {
                harvesterService.harvest(repo);
            } catch (Exception e) {
                log.error("Unable to process {}", repo, e);
            }
        }
    }
}
