package it.gov.innovazione.ndc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.Charset.defaultCharset;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!int-test") // Only run this initializer in non-test profiles
public class SynonymsElasticsearchIndexInitializer {

    public static final String INDEX_NAME = "semantic-asset-metadata-9";
    public static final String ELASTICSEARCH_SETTINGS_JSON = "elasticsearch-settings.json";
    public static final String SYNONYMS_TXT = "synonyms.txt";

    private static final Pattern INDEX_VERSION_PATTERN = Pattern.compile("^(.*)-(\\d+)$");

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void createIndexWithSynonyms() throws Exception {

        // 1) Parse prefix e versione da INDEX_NAME (es. "semantic-asset-metadata-9" -> prefix="semantic-asset-metadata", version=9)
        final Optional<ParsedIndex> optionalParsedIndex = parseIndexName();
        if (optionalParsedIndex.isPresent()) {
            cleanupPreviousIndexes(optionalParsedIndex.get());
        } else {
            log.warn("INDEX_NAME '{}' non termina con -<numero>; skip cleanup vecchi indici.", INDEX_NAME);
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(SemanticAssetMetadata.class);

        if (indexOps.exists()) {
            log.info("Index {} already exists, skipping creation.", INDEX_NAME);
            return;
        }

        List<String> synonyms = loadSynonymsFromClasspath()
                .orElse(Collections.emptyList());

        log.info("Loaded {} synonyms from {}", synonyms.size(), SYNONYMS_TXT);

        Map<String, Object> settings = loadSettingsJson();

        if (settings == null) {
            throw new IllegalStateException("Unable to load elasticsearch-settings.json from classpath");
        }

        Map<String, Object> analysis = (Map<String, Object>) settings.get("analysis");
        Map<String, Object> filter = (Map<String, Object>) analysis.get("filter");
        Map<String, Object> synonymFilter = (Map<String, Object>) filter.get("italian_synonyms");

        synonymFilter.put("synonyms", synonyms);

        Map<String, Object> indexSettings = Map.of(
                "index", Map.of(
                        "max_ngram_diff", 12,
                        "number_of_shards", 1,
                        "number_of_replicas", 0,
                        "analysis", analysis));

        safelyCreateIndexAndPutMappings(indexOps, indexSettings);
    }

    private void safelyCreateIndexAndPutMappings(IndexOperations indexOps, Map<String, Object> indexSettings) {
        try {
            indexOps.create(indexSettings);
            indexOps.putMapping(indexOps.createMapping());
            log.info("Index {} created with settings and mapping.", INDEX_NAME);
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("resource_already_exists_exception")) {
                log.warn("Index {} already created by another instance. Continuing.", INDEX_NAME);
            } else {
                throw e;
            }
        }
    }

    private Optional<List<String>> loadSynonymsFromClasspath() {
        try {
            ClassPathResource resource = new ClassPathResource(SYNONYMS_TXT);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), defaultCharset()))) {
                return Optional.of(reader.lines().collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.warn("Failed to load synonyms", e);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadSettingsJson() throws Exception {
        return objectMapper.readValue(new ClassPathResource(ELASTICSEARCH_SETTINGS_JSON).getInputStream(), Map.class);
    }

    private Optional<ParsedIndex> parseIndexName() {
        try {
            Matcher m = INDEX_VERSION_PATTERN.matcher(INDEX_NAME);

            if (!m.matches()) {
                return Optional.empty();
            }
            String prefix = m.group(1);
            int version = Integer.parseInt(m.group(2));
            return Optional.of(new ParsedIndex(prefix, version));
        } catch (Exception e) {
            log.warn("Failed to parse index name '{}'", INDEX_NAME, e);
            return Optional.empty();
        }

    }

    private void cleanupPreviousIndexes(ParsedIndex parsedIndex) {
        String prefix = parsedIndex.prefix;
        int version = parsedIndex.version;
        if (version <= 0) {
            log.info("Index version is {}, no previous indexes to cleanup.", version);
            return;
        }
        for (int v = 0; v < version; v++) {
            String idx = prefix + "-" + v;
            try {
                IndexOperations ops = elasticsearchOperations.indexOps(IndexCoordinates.of(idx));
                if (ops.exists()) {
                    boolean deleted = ops.delete();
                    log.info("Deleted old index {} (deleted={})", idx, deleted);
                } else {
                    log.info("Old index {} not found, skipping", idx);
                }
            } catch (Exception e) {
                // non facciamo fallire lo startup per cleanup; logghiamo e proseguiamo
                log.warn("Failed to delete old index {}. Skipping.", idx, e);
            }
        }
        log.info("Cleanup complete for prefix '{}' up to version {}.", prefix, version - 1);
    }

    private record ParsedIndex(String prefix, int version) {
    }


}
