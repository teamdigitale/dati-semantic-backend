package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.controller.AggregateDashboardResponse;
import it.gov.innovazione.ndc.controller.date.DateParameter;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.model.harvester.SemanticContentStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.FAILURE;
import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.SUCCESS;
import static it.gov.innovazione.ndc.model.harvester.HarvesterRun.Status.UNCHANGED;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String REPO_ID = "repo-1";
    private static final String SUCCESSFUL_RUN_ID = "run-success";

    @Mock
    private DashboardRepo dashboardRepo;

    @InjectMocks
    private DashboardService dashboardService;

    private static HarvesterRun run(String id, String startedAt, HarvesterRun.Status status) {
        return HarvesterRun.builder()
                .id(id)
                .repositoryId(REPO_ID)
                .repositoryUrl("https://github.com/example/repo")
                .startedAt(Instant.parse(startedAt))
                .endedAt(Instant.parse(startedAt))
                .status(status)
                .build();
    }

    private static SemanticContentStats stats(String resourceUri, SemanticAssetType type) {
        return SemanticContentStats.builder()
                .id(resourceUri)
                .harvesterRunId(SUCCESSFUL_RUN_ID)
                .resourceUri(resourceUri)
                .resourceType(type)
                .rightHolder("agid")
                .status(emptyList())
                .build();
    }

    @BeforeEach
    void setUp() {
        lenient().when(dashboardRepo.getRepoById())
                .thenReturn(Map.of(REPO_ID, Repository.builder().id(REPO_ID).build()));
        lenient().when(dashboardRepo.getAllStats()).thenReturn(List.of(
                stats("https://w3id.org/italia/onto/CPV", SemanticAssetType.ONTOLOGY),
                stats("https://w3id.org/italia/controlled-vocabulary/sex", SemanticAssetType.CONTROLLED_VOCABULARY)));
    }

    @Test
    void shouldCountAssetsOfTheLatestSuccessfulRun() {
        givenRuns(run(SUCCESSFUL_RUN_ID, "2026-03-01T10:00:00Z", SUCCESS));

        assertThat(countsByType()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ontology", 1L,
                "controlled vocabulary", 1L));
    }

    @Test
    void shouldIgnoreLaterUnchangedRunWhenCounting() {
        // a periodic harvest finding no new revision ends UNCHANGED and writes no content stats:
        // picking it as the latest run of the repository would blank the whole dashboard
        givenRuns(
                run(SUCCESSFUL_RUN_ID, "2026-03-01T10:00:00Z", SUCCESS),
                run("run-unchanged", "2026-03-02T20:00:00Z", UNCHANGED));

        assertThat(countsByType()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ontology", 1L,
                "controlled vocabulary", 1L));
    }

    @Test
    void shouldIgnoreLaterFailedRunWhenCounting() {
        givenRuns(
                run(SUCCESSFUL_RUN_ID, "2026-03-01T10:00:00Z", SUCCESS),
                run("run-failure", "2026-03-02T20:00:00Z", FAILURE));

        assertThat(countsByType()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ontology", 1L,
                "controlled vocabulary", 1L));
    }

    @Test
    void shouldReturnNoRowsWhenTheRepositoryWasNeverHarvestedSuccessfully() {
        givenRuns(run("run-unchanged", "2026-03-02T20:00:00Z", UNCHANGED));

        assertThat(aggregateCountData().getRows()).isEmpty();
    }

    private void givenRuns(HarvesterRun... runs) {
        when(dashboardRepo.getAllRuns()).thenReturn(List.of(runs));
    }

    private AggregateDashboardResponse aggregateCountData() {
        return dashboardService.getAggregateCountData(
                DateParameter.of("2026", null, null, DateParameter.Granularity.YEARS),
                List.of(DimensionalItem.CountDataDimensionalItem.RESOURCE_TYPE),
                emptyList());
    }

    private Map<String, Long> countsByType() {
        return aggregateCountData().getRows().stream()
                .collect(toMap(row -> (String) row.get(1), row -> (Long) row.get(2)));
    }
}
