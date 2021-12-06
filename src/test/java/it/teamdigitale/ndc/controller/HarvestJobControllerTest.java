package it.teamdigitale.ndc.controller;

import it.teamdigitale.ndc.harvester.HarvesterJob;
import it.teamdigitale.ndc.harvester.HarvesterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HarvestJobControllerTest {

    @Mock
    HarvesterService harvesterService;
    @Mock
    HarvesterJob harvesterJob;
    @InjectMocks
    HarvestJobController harvestJobController;

    @Test
    void shouldStartHarvestJobForAllRepositories() {
        harvestJobController.startHarvestJob();
        verify(harvesterJob).harvest();
    }

    @Test
    void shouldStartHarvestForSingleRepository() throws IOException {
        String repoUrl = "http://github.com/repo";
        harvestJobController.csv(repoUrl);
        verify(harvesterService).harvest(repoUrl);
    }

}