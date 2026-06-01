package it.gov.innovazione.ndc.controller.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.model.audit.ChangeKind;
import it.gov.innovazione.ndc.model.audit.ResourceDelta;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository.ChangelogResult;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository.ChangelogRow;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository.DeltaFilters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/semantic-assets")
public class SemanticAssetChangelogController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ResourceDeltaRepository deltaRepository;

    @GetMapping("/changelog")
    @Operation(
            operationId = "getSemanticAssetChangelog",
            description = "Time-series of delta entries for a semantic asset, cross-repo, ordered by createdAt DESC.",
            summary = "Changelog of a semantic asset")
    public ResponseEntity<SemanticAssetChangelogPage> getChangelog(
            @RequestParam String iri,
            @RequestParam(required = false) List<ChangeKind> changeKind,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant until,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {

        if (iri == null || iri.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int safeOffset = sanitizeOffset(offset);
        int safeLimit = sanitizeLimit(limit);

        DeltaFilters filters = DeltaFilters.builder()
                .changeKinds(changeKind)
                .build();

        ChangelogResult result = deltaRepository.findChangelog(iri, filters, since, until, safeOffset, safeLimit);

        List<ChangelogEntry> entries = result.content().stream()
                .map(SemanticAssetChangelogController::toEntry)
                .toList();

        SemanticAssetType firstAssetType = result.content().stream()
                .map(ChangelogRow::delta)
                .map(ResourceDelta::getAssetType)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);

        return ResponseEntity.ok(SemanticAssetChangelogPage.builder()
                .assetIri(iri)
                .assetType(firstAssetType)
                .content(entries)
                .offset(safeOffset)
                .limit(safeLimit)
                .total(result.total())
                .build());
    }

    private static ChangelogEntry toEntry(ChangelogRow row) {
        ResourceDelta d = row.delta();
        return ChangelogEntry.builder()
                .runId(d.getHarvesterRunId())
                .repositoryId(row.repositoryId())
                .revision(row.revision())
                .revisionCommittedAt(row.revisionCommittedAt())
                .createdAt(d.getCreatedAt())
                .changeKind(d.getChangeKind())
                .summary(parseJson(d.getSummaryJson()))
                .build();
    }

    private static JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return JSON.readTree(raw);
        } catch (JsonProcessingException e) {
            return JSON.getNodeFactory().textNode(raw);
        }
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
}
