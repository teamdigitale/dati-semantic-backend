package it.gov.innovazione.ndc.harvester.service.startupjob;

import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import it.gov.innovazione.ndc.service.logging.NDCHarvesterLoggerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TempEraserStartupJob implements StartupJob {

    private final HarvesterService harvesterService;

    @Override
    public void run() {
        NDCHarvesterLoggerUtils.setInitialContext(LoggingContext.builder()
                .component("TempEraserStartupJob")
                .build());
        harvesterService.cleanTempGraphsForConfiguredRepo();
    }

}
