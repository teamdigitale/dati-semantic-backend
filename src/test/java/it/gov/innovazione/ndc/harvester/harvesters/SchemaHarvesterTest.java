package it.gov.innovazione.ndc.harvester.harvesters;

import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.pathprocessors.SchemaPathProcessor;
import it.gov.innovazione.ndc.harvester.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaHarvesterTest {
    @Mock
    AgencyRepositoryService agencyRepositoryService;
    @Mock
    SchemaPathProcessor pathProcessor;
    @Mock
    ConfigService configService;
    @InjectMocks
    SchemaHarvester harvester;

    @Test
    void shouldProcessAllScannedPaths() {
        final Path schemaBasePath = Path.of("assets/schemas");
        final String repoUrl = "my-repo.git";
        final SemanticAssetPath path1 = SemanticAssetPath.of("schema1.ttl");
        final SemanticAssetPath path2 = SemanticAssetPath.of("schema2.ttl");
        when(configService.findParsedOrGetDefault(any())).thenReturn(Optional.empty());
        when(agencyRepositoryService.getSchemaPaths(schemaBasePath)).thenReturn(List.of(path1, path2));

        harvester.harvest(asRepo(repoUrl), schemaBasePath);

        verify(agencyRepositoryService).getSchemaPaths(schemaBasePath);
        verify(pathProcessor).process(repoUrl, path1);
        verify(pathProcessor).process(repoUrl, path2);
    }

}
