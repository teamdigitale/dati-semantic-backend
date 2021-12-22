package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.harvester.HarvesterService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;
    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    @Autowired
    private HarvesterService harvesterService;

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
        return new HarvestRepositoryProcessor(harvesterService);
    }
}
