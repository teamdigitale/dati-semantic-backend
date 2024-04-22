package it.gov.innovazione.ndc.integration;

import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import it.gov.innovazione.ndc.harvester.service.ConfigReaderService;
import it.gov.innovazione.ndc.harvester.service.ConfigService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.repository.TripleStoreProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static it.gov.innovazione.ndc.harvester.service.RepositoryUtils.asRepo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseIntegrationTest {

    private static final ElasticsearchContainer elasticsearchContainer =
        Containers.buildElasticsearchContainer();
    private static final GenericContainer virtuoso = Containers.buildVirtuosoContainer();
    private boolean harvested = false;
    private static final String REPO_URL = "http://testRepoURL";

    @LocalServerPort
    int port;

    @SpyBean
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    HarvesterService harvesterService;

    @SpyBean
    AgencyRepositoryService agencyRepositoryService;

    @SpyBean
    RepositoryService repositoryService;

    @Autowired
    TripleStoreProperties virtuosoProps;

    @BeforeAll
    public static void beforeAll() {
        virtuoso.start();
        elasticsearchContainer.start();
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        if (!harvested) {
            dataIsHarvested();
            harvested = true;
        }
    }

    static void updateTestcontainersProperties(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + virtuoso.getMappedPort(Containers.VIRTUOSO_PORT);
        String elasticSearchAddress = elasticsearchContainer.getHttpHostAddress();
        registry.add("virtuoso.sparql", () -> url + "/sparql");
        registry.add("virtuoso.sparql-graph-store", () -> url + "/sparql-graph-crud/");
        registry.add("spring.elasticsearch.rest.uris", () -> elasticSearchAddress);
    }

    private void dataIsHarvested() throws IOException {
        String dir = "src/test/resources/testdata";
        Path cloneDir = Path.of(dir);
        doReturn(cloneDir).when(agencyRepositoryService).cloneRepo(REPO_URL, null);
        doNothing().when(agencyRepositoryService).removeClonedRepo(cloneDir);
        doNothing().when(repositoryService).storeRightsHolders(any(), any());

        harvesterService.harvest(asRepo(REPO_URL));

        refreshAllIndicesUsedForBulkIndexing();

        elasticsearchOperations.indexOps(SemanticAssetMetadata.class).refresh();
    }

    private void refreshAllIndicesUsedForBulkIndexing() {
        ArgumentCaptor<IndexCoordinates> indexCaptor = ArgumentCaptor.forClass(IndexCoordinates.class);

        verify(elasticsearchOperations).bulkIndex(anyList(), indexCaptor.capture());
        Set<IndexCoordinates> referencedIndices = new HashSet<>(indexCaptor.getAllValues());
        referencedIndices.forEach(ic -> elasticsearchOperations.indexOps(ic).refresh());
    }
}
