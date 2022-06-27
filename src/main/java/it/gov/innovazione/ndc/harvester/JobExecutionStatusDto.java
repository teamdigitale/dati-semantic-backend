package it.gov.innovazione.ndc.harvester;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonInclude(NON_NULL)
public class JobExecutionStatusDto {
    private final JobInstance jobInstance;
    private final String startTime;
    private final String endTime;
    private final JobParameters jobParameters;
    private final BatchStatus status;
    private final ExitStatus exitStatus;

    public JobExecutionStatusDto(JobExecution jobExecution) {
        jobInstance = jobExecution.getJobInstance();
        jobParameters = jobExecution.getJobParameters();
        startTime = Optional.ofNullable(jobExecution.getStartTime()).map(Object::toString).orElse("");
        endTime = Optional.ofNullable(jobExecution.getEndTime()).map(Object::toString).orElse("");
        status = jobExecution.getStatus();
        exitStatus = jobExecution.getExitStatus();
    }
}
