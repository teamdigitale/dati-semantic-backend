package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.harvester.HarvesterJob;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HarvestJobControllerTest {

    @Mock
    HarvesterJob harvesterJob;
    @Mock
    HarvesterService harvesterService;
    @Mock
    NdcEventPublisher eventPublisher;
    @InjectMocks
    HarvestJobController harvestJobController;

    @Test
    void shouldStartHarvestForSpecifiedRepositories() {
        String repoUrls = "http://github.com/repo,http://github.com/repo2";
        harvestJobController.harvestRepositories(repoUrls, "", false);
        verify(harvesterJob).harvest(repoUrls, "", false);
    }

    @Test
    void shouldSynchronouslyClearRepo() {
        String repoUrl = "http://github.com/repo.git";
        harvestJobController.clearRepo(repoUrl);
        verify(harvesterService).clear(repoUrl);
    }
}
