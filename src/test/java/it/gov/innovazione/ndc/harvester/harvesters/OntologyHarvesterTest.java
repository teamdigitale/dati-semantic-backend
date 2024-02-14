package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.pathprocessors.OntologyPathProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OntologyHarvesterTest {
    @Mock
    AgencyRepositoryService agencyRepositoryService;
    @Mock
    OntologyPathProcessor pathProcessor;
    @InjectMocks
    OntologyHarvester harvester;

    @Test
    void shouldProcessAllScannedPaths() {
        final Path ontologyBasePath = Path.of("assets/ontologies");
        final String repoUrl = "my-repo.git";
        final SemanticAssetPath path1 = SemanticAssetPath.of("onto1.ttl");
        final SemanticAssetPath path2 = SemanticAssetPath.of("onto2.ttl");
        when(agencyRepositoryService.getOntologyPaths(ontologyBasePath)).thenReturn(List.of(path1, path2));

        harvester.harvest(asRepo(repoUrl), ontologyBasePath);

        verify(agencyRepositoryService).getOntologyPaths(ontologyBasePath);
        verify(pathProcessor).process(repoUrl, path1);
        verify(pathProcessor).process(repoUrl, path2);
    }
}
