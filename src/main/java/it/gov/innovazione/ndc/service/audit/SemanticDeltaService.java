package it.gov.innovazione.ndc.service.audit;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.model.audit.ChangeKind;
import it.gov.innovazione.ndc.model.audit.ResourceDelta;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.ResourceDeltaRepository;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static it.gov.innovazione.ndc.repository.TripleStoreRepository.TMP_GRAPH_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticDeltaService {

    private static final SemanticAssetType[] SUPPORTED_TYPES = {SemanticAssetType.ONTOLOGY};

    private final TripleStoreRepository tripleStoreRepository;
    private final ResourceDeltaRepository resourceDeltaRepository;
    private final List<AssetDeltaClassifier> classifiers;

    public int computeAndPersistDelta(Repository repository, String runId) {
        String repoUrl = normalize(repository.getUrl());
        String onlineGraph = repoUrl;
        String tmpGraph = withPrefix(repoUrl, TMP_GRAPH_PREFIX);
        log.info("Computing semantic delta for run {} on repo {}", runId, repoUrl);

        int totalRows = 0;
        for (SemanticAssetType type : SUPPORTED_TYPES) {
            try {
                totalRows += computeForType(runId, type, tmpGraph, onlineGraph);
            } catch (Exception e) {
                log.error("Failed delta computation for run {} type {}: {}", runId, type, e.getMessage(), e);
            }
        }
        log.info("Delta computation for run {} produced {} rows", runId, totalRows);
        return totalRows;
    }

    private int computeForType(String runId, SemanticAssetType type, String tmpGraph, String onlineGraph) {
        AssetDeltaClassifier classifier = classifiers.stream()
                .filter(c -> c.supports(type))
                .findFirst()
                .orElse(null);
        if (classifier == null) {
            log.debug("No classifier for {}, skipping", type);
            return 0;
        }

        Set<String> tmpIris = new HashSet<>(listAssetIris(tmpGraph, type));
        Set<String> onlineIris = new HashSet<>(listAssetIris(onlineGraph, type));
        log.debug("Type {}: tmp={} assets, online={} assets", type, tmpIris.size(), onlineIris.size());

        List<ResourceDelta> rows = new ArrayList<>();

        // ADDED: in tmp but not in online
        for (String iri : tmpIris) {
            if (onlineIris.contains(iri)) {
                continue;
            }
            Model assetModel = describeAsset(tmpGraph, iri);
            String summary = classifier.summarizeAdded(iri, assetModel);
            rows.add(buildRow(runId, iri, type, ChangeKind.ADDED, summary));
        }

        // REMOVED: in online but not in tmp
        for (String iri : onlineIris) {
            if (tmpIris.contains(iri)) {
                continue;
            }
            Model assetModel = describeAsset(onlineGraph, iri);
            String summary = classifier.summarizeRemoved(iri, assetModel);
            rows.add(buildRow(runId, iri, type, ChangeKind.REMOVED, summary));
        }

        // MODIFIED: in both, but with triple differences
        for (String iri : tmpIris) {
            if (!onlineIris.contains(iri)) {
                continue;
            }
            Model tmpModel = describeAsset(tmpGraph, iri);
            Model onlineModel = describeAsset(onlineGraph, iri);
            Model added = tmpModel.difference(onlineModel);
            Model removed = onlineModel.difference(tmpModel);
            Optional<String> summary = classifier.classifyModified(iri, added, removed);
            if (summary.isEmpty()) {
                continue;
            }
            rows.add(buildRow(runId, iri, type, ChangeKind.MODIFIED, summary.get()));
        }

        resourceDeltaRepository.saveAll(rows);
        return rows.size();
    }

    private List<String> listAssetIris(String graphIri, SemanticAssetType type) {
        String sparql = String.format(
                "SELECT DISTINCT ?asset WHERE { GRAPH <%s> { ?asset a <%s> } }",
                graphIri, type.getTypeIri());
        try {
            return tripleStoreRepository.selectStrings(sparql, "asset");
        } catch (Exception e) {
            log.warn("Could not list assets from graph {} (probably non-existent): {}", graphIri, e.getMessage());
            return List.of();
        }
    }

    private Model describeAsset(String graphIri, String assetIri) {
        String sparql = String.format(
                "CONSTRUCT { ?s ?p ?o } WHERE { "
                        + "GRAPH <%s> { "
                        + "  ?s ?p ?o . "
                        + "  FILTER( ?s = <%s> "
                        + "    || STRSTARTS(STR(?s), \"%s\") "
                        + "    || EXISTS { ?s <http://www.w3.org/2000/01/rdf-schema#isDefinedBy> <%s> } ) "
                        + "} }",
                graphIri, assetIri, assetIri, assetIri);
        try {
            return tripleStoreRepository.construct(sparql);
        } catch (Exception e) {
            log.warn("Could not CONSTRUCT asset {} from graph {}: {}", assetIri, graphIri, e.getMessage());
            return tripleStoreRepository.emptyModel();
        }
    }

    private ResourceDelta buildRow(String runId, String iri, SemanticAssetType type, ChangeKind kind, String summary) {
        return ResourceDelta.builder()
                .id(UUID.randomUUID().toString())
                .harvesterRunId(runId)
                .assetIri(iri)
                .assetType(type)
                .changeKind(kind)
                .summaryJson(summary)
                .createdAt(Instant.now())
                .build();
    }

    private static String normalize(String url) {
        return url.replace(".git", "");
    }

    private static String withPrefix(String repoUrl, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return repoUrl;
        }
        try {
            URL url = new URL(repoUrl);
            String port = url.getPort() == -1 ? "" : ":" + url.getPort();
            return url.getProtocol() + "://" + prefix + "." + url.getHost() + port + url.getPath();
        } catch (MalformedURLException e) {
            return repoUrl;
        }
    }
}
