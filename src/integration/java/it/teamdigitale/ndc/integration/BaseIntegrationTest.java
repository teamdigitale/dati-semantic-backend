package it.teamdigitale.ndc.integration;

import it.teamdigitale.ndc.harvester.AgencyRepositoryService;
import it.teamdigitale.ndc.harvester.HarvesterService;
import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import it.teamdigitale.ndc.repository.TripleStoreProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class BaseIntegrationTest {

    private static final ElasticsearchContainer elasticsearchContainer =
            Containers.buildElasticsearchContainer();
    private static final GenericContainer virtuoso = Containers.buildVirtuosoContainer();
    private boolean harvested = false;
    private String repositoryurl = "http://testRepoURL";

    @LocalServerPort
    int port;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    HarvesterService harvesterService;

    @SpyBean
    AgencyRepositoryService agencyRepositoryService;

    @Autowired
    TripleStoreProperties virtuosoProps;

    @BeforeAll
    public static void beforeAll() {
        virtuoso.start();
        elasticsearchContainer.start();
    }

    @BeforeEach
    public synchronized void beforeEach() throws IOException {
        if (!harvested) {
            dataIsHarvested();
            harvested = true;
        }
    }

    public static void tearDown() {
        virtuoso.stop();
        elasticsearchContainer.stop();
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
        doReturn(cloneDir).when(agencyRepositoryService).cloneRepo(repositoryurl);
        doNothing().when(agencyRepositoryService).removeClonedRepo(cloneDir);

        harvesterService.harvest(repositoryurl);

        elasticsearchOperations.indexOps(SemanticAssetMetadata.class).refresh();
    }
}
