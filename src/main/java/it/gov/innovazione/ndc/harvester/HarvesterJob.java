package it.gov.innovazione.ndc.harvester;

import it.gov.innovazione.ndc.config.SimpleHarvestRepositoryProcessor;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.harvester.util.GitUtils;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.HarvestJobException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HarvesterJob {

    private final RepositoryService repositoryService;
    private final SimpleHarvestRepositoryProcessor simpleHarvestRepositoryProcessor;
    private final GitUtils gitUtils;

    public List<JobExecutionResponse> harvest(Boolean force) {
        List<Repository> allRepos = repositoryService.getActiveRepos();
        return harvest(allRepos, force);
    }

    public List<JobExecutionResponse> harvest(List<Repository> repositories, Boolean force) {
        String correlationId = UUID.randomUUID().toString();
        List<JobExecutionResponse> responses = new ArrayList<>();
        for (Repository repository : repositories) {
            responses.add(harvest(repository, correlationId, force));
        }
        return responses;
    }

    public List<JobExecutionResponse> harvest(List<Repository> repos) {
        return harvest(repos, false);
    }

    public List<JobExecutionResponse> harvest() {
        return harvest(false);
    }

    private JobExecutionResponse harvest(Repository repository, String correlationId, boolean force) {
        return harvest(repository, correlationId, null, force);
    }

    public JobExecutionResponse harvest(String repositoryId, String revision, Boolean force) {
        Repository repository = repositoryService.findRepoById(repositoryId)
                .orElseThrow(() -> new HarvestJobException(String.format("Repository %s not found", repositoryId)));
        String correlationId = UUID.randomUUID().toString();
        return harvest(repository, correlationId, revision, force);
    }


    private JobExecutionResponse harvest(Repository repository, String correlationId, String revision, boolean force) {

        String runId = UUID.randomUUID().toString();

        JobExecutionResponse.JobExecutionResponseBuilder responseBuilder = JobExecutionResponse.builder()
                .runId(runId)
                .correlationId(correlationId)
                .repositoryId(repository.getId())
                .repositoryUrl(repository.getUrl())
                .startedAt(Instant.now().toString())
                .forced(force);

        revision = Optional.ofNullable(revision)
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> gitUtils.getHeadRemoteRevision(repository.getUrl()));

        simpleHarvestRepositoryProcessor.execute(runId, repository, correlationId, revision, force, SecurityUtils.getCurrentUserLogin());

        log.info("Harvest job started at " + LocalDateTime.now());

        return responseBuilder.build();

    }

}
