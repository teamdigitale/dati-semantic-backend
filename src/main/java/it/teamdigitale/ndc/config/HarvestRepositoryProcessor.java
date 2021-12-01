package it.teamdigitale.ndc.config;

import it.teamdigitale.ndc.harvester.HarvesterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

@Slf4j
public class HarvestRepositoryProcessor implements Tasklet {
    private final HarvesterService harvesterService;
    private final List<String> repos;

    public HarvestRepositoryProcessor(HarvesterService harvesterService, List<String> repos) {
        this.harvesterService = harvesterService;
        this.repos = repos;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        for (String repo : repos) {
            try {
                harvesterService.harvest(repo);
            } catch (Exception e) {
                log.error("Unable to process {}", repo, e);
            }
        }
        return RepeatStatus.FINISHED;
    }
}
