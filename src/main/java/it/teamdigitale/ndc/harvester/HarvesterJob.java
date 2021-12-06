package it.teamdigitale.ndc.harvester;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Configuration
@EnableScheduling
@Slf4j
public class HarvesterJob {
    private final JobLauncher jobLauncher;
    private final Job harvestSemanticAssetsJob;
    private final Clock clock;

    @Autowired
    public HarvesterJob(JobLauncher jobLauncher,
                        Job harvestSemanticAssetsJob,
                        Clock clock) {
        this.jobLauncher = jobLauncher;
        this.harvestSemanticAssetsJob = harvestSemanticAssetsJob;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 22 ? * *")
    public void harvest() {
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:00");
            String currentDateTime = now.format(formatter);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("harvestTime", currentDateTime).toJobParameters();
            jobLauncher.run(harvestSemanticAssetsJob, jobParameters);
        } catch (Exception e) {
            log.error("Error in harvest job ", e);
        }
    }
}
