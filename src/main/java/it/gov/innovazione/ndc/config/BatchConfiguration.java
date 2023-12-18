package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;
    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    @Autowired
    private HarvesterService harvesterService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private HarvesterRunService harvesterRunService;
    @Autowired
    private NdcEventPublisher ndcEventPublisher;

    @Bean
    public Job harvestSemanticAssetsJob() {
        return jobBuilderFactory.get("harvestSemanticAssetsJob")
                .start(harvestStep())
                .build();
    }

    @Bean
    public Step harvestStep() {
        return stepBuilderFactory.get("harvestStep")
                .tasklet(harvestRepositoryProcessor())
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public HarvestRepositoryProcessor harvestRepositoryProcessor() {
        return new HarvestRepositoryProcessor(
                harvesterService,
                repositoryService,
                harvesterRunService,
                ndcEventPublisher);
    }

    @Bean
    @Primary
    public JobLauncher simpleJobLauncher(JobRepository jobRepository) throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(taskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(10);
        return taskExecutor;
    }
}
