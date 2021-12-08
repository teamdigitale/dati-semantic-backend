package it.teamdigitale.ndc.config;

import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.repository.HarvestJobException;
import it.teamdigitale.ndc.repository.TripleStoreRepositoryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
public class HarvestRepositoryProcessor implements Tasklet, StepExecutionListener {
    private final HarvesterService harvesterService;
    private List<String> repos;
    private List<String> failedRepos;

    public HarvestRepositoryProcessor(HarvesterService harvesterService) {
        this.harvesterService = harvesterService;
    }

    // Used for Testing
    public HarvestRepositoryProcessor(HarvesterService harvesterService, List<String> repos) {
        this.harvesterService = harvesterService;
        this.repos = repos;
        this.failedRepos = new ArrayList<>();
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters parameters = stepExecution.getJobExecution().getJobParameters();
        String repositories = parameters.getString("repositories");
        this.repos = isNull(repositories) ? new ArrayList<>() : Arrays.asList(repositories.split(","));
        this.failedRepos = new ArrayList<>();
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        for (String repo : repos) {
            try {
                harvesterService.harvest(repo);
            } catch (Exception e) {
                log.error("Unable to process {}", repo, e);
                failedRepos.add(repo);
            }
        }
        if (!failedRepos.isEmpty()) {
            throw new HarvestJobException(String.format("Harvesting failed for repos '%s'", repos));
        }
        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return failedRepos.isEmpty() ? ExitStatus.COMPLETED : ExitStatus.FAILED;
    }
}
