package it.gov.innovazione.ndc.harvester.service.startupjob;

import it.gov.innovazione.ndc.harvester.HarvesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TempEraserStartupJob implements StartupJob {

    private final HarvesterService harvesterService;

    @Override
    public void run() {
        harvesterService.cleanTempGraphsForConfiguredRepo();
    }

}
