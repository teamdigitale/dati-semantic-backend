package it.gov.innovazione.ndc.controller.audit;

import io.swagger.v3.oas.annotations.Operation;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.service.HarvesterRunService;
import it.gov.innovazione.ndc.model.audit.ChangeKind;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository.DeltaFilters;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository.DeltaQueryResult;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository.DeltaSummaryCounters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/config/repository/{id}")
public class ResourceDeltaController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final ResourceDeltaRepository deltaRepository;
    private final HarvesterRunService harvesterRunService;

    @GetMapping("/runs/{runId}/delta")
    @Operation(
            operationId = "getRunDelta",
            description = "Get the semantic delta of a specific harvester run, grouped by asset.",
            summary = "Semantic delta for a harvester run")
    public ResponseEntity<ResourceDeltaPage> getRunDelta(
            @PathVariable String id,
            @PathVariable String runId,
            @RequestParam(required = false) List<ChangeKind> changeKind,
            @RequestParam(required = false) List<SemanticAssetType> assetType,
            @RequestParam(required = false) String assetIri,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {

        Optional<HarvesterRun> run = findRun(id, runId);
        if (run.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildPage(run.get(), changeKind, assetType, assetIri, offset, limit));
    }

    @GetMapping("/runs/latest/delta")
    @Operation(
            operationId = "getLatestRunDelta",
            description = "Get the semantic delta of the latest SUCCESS run for the repository.",
            summary = "Semantic delta for the latest successful run")
    public ResponseEntity<ResourceDeltaPage> getLatestRunDelta(
            @PathVariable String id,
            @RequestParam(required = false) List<ChangeKind> changeKind,
            @RequestParam(required = false) List<SemanticAssetType> assetType,
            @RequestParam(required = false) String assetIri,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {

        Optional<HarvesterRun> latest = latestSuccessRun(id);
        int safeOffset = sanitizeOffset(offset);
        int safeLimit = sanitizeLimit(limit);
        if (latest.isEmpty()) {
            return ResponseEntity.ok(ResourceDeltaPage.builder()
                    .run(null)
                    .content(List.of())
                    .offset(safeOffset)
                    .limit(safeLimit)
                    .total(0)
                    .build());
        }
        return ResponseEntity.ok(buildPage(latest.get(), changeKind, assetType, assetIri, offset, limit));
    }

    @GetMapping("/runs/{runId}/delta/summary")
    @Operation(
            operationId = "getRunDeltaSummary",
            description = "Get aggregate counters of the semantic delta of a harvester run.",
            summary = "Summary of the delta for a harvester run")
    public ResponseEntity<ResourceDeltaSummary> getRunDeltaSummary(
            @PathVariable String id,
            @PathVariable String runId) {

        Optional<HarvesterRun> run = findRun(id, runId);
        if (run.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildSummary(run.get()));
    }

    @GetMapping("/runs/latest/delta/summary")
    @Operation(
            operationId = "getLatestRunDeltaSummary",
            description = "Get aggregate counters for the latest SUCCESS run of the repository.",
            summary = "Summary of the delta for the latest successful run")
    public ResponseEntity<ResourceDeltaSummary> getLatestRunDeltaSummary(@PathVariable String id) {
        Optional<HarvesterRun> latest = latestSuccessRun(id);
        if (latest.isEmpty()) {
            return ResponseEntity.ok(ResourceDeltaSummary.empty(null));
        }
        return ResponseEntity.ok(buildSummary(latest.get()));
    }

    private ResourceDeltaPage buildPage(HarvesterRun run,
                                        List<ChangeKind> changeKind,
                                        List<SemanticAssetType> assetType,
                                        String assetIri,
                                        Integer offset,
                                        Integer limit) {
        DeltaFilters filters = DeltaFilters.builder()
                .changeKinds(changeKind)
                .assetTypes(assetType)
                .assetIri(assetIri)
                .build();
        int safeOffset = sanitizeOffset(offset);
        int safeLimit = sanitizeLimit(limit);
        DeltaQueryResult result = deltaRepository.findByRun(run.getId(), filters, safeOffset, safeLimit);
        List<ResourceDeltaItem> items = result.content().stream()
                .map(ResourceDeltaItem::of)
                .toList();
        return ResourceDeltaPage.builder()
                .run(RunInfo.of(run))
                .content(items)
                .offset(safeOffset)
                .limit(safeLimit)
                .total(result.total())
                .build();
    }

    private ResourceDeltaSummary buildSummary(HarvesterRun run) {
        Optional<DeltaSummaryCounters> counters = deltaRepository.summarizeByRun(run.getId());
        ResourceDeltaSummary template = ResourceDeltaSummary.empty(RunInfo.of(run));
        if (counters.isEmpty()) {
            return template;
        }
        Map<SemanticAssetType, Map<ChangeKind, Integer>> cross = new LinkedHashMap<>(template.getCrossTab());
        Map<ChangeKind, Integer> byKind = new EnumMap<>(template.getByChangeKind());
        Map<SemanticAssetType, Integer> byType = new EnumMap<>(template.getByAssetType());

        counters.get().getByAssetTypeAndKind().forEach((typeName, kindMap) -> {
            SemanticAssetType t = safeAssetType(typeName);
            if (t == null) {
                return;
            }
            kindMap.forEach((kindName, count) -> {
                ChangeKind k = safeChangeKind(kindName);
                if (k == null) {
                    return;
                }
                cross.get(t).merge(k, count, Integer::sum);
                byKind.merge(k, count, Integer::sum);
                byType.merge(t, count, Integer::sum);
            });
        });

        return ResourceDeltaSummary.builder()
                .run(RunInfo.of(run))
                .byChangeKind(byKind)
                .byAssetType(byType)
                .crossTab(cross)
                .build();
    }

    private Optional<HarvesterRun> findRun(String repoId, String runId) {
        return harvesterRunService.getAllRuns().stream()
                .filter(r -> repoId.equals(r.getRepositoryId()))
                .filter(r -> runId.equals(r.getId()))
                .findFirst();
    }

    private Optional<HarvesterRun> latestSuccessRun(String repoId) {
        return harvesterRunService.getAllRuns().stream()
                .filter(r -> repoId.equals(r.getRepositoryId()))
                .filter(r -> r.getStatus() == HarvesterRun.Status.SUCCESS)
                .max(Comparator.comparing(HarvesterRun::getStartedAt));
    }

    private static int sanitizeOffset(Integer offset) {
        if (offset == null || offset < 0) {
            return 0;
        }
        return offset;
    }

    private static int sanitizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static SemanticAssetType safeAssetType(String name) {
        try {
            return SemanticAssetType.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static ChangeKind safeChangeKind(String name) {
        try {
            return ChangeKind.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static List<ChangeKind> toList(ChangeKind k) {
        List<ChangeKind> l = new ArrayList<>();
        l.add(k);
        return l;
    }
}
