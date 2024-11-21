package it.gov.innovazione.ndc.harvester.service.startupjob;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StartupJobsRunner {

    private final List<StartupJob> startupJobs;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        startupJobs.forEach(StartupJob::run);
    }

}
