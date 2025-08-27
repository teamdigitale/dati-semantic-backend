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
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void createIndexWithSynonyms() throws Exception {

        IndexOperations indexOps = elasticsearchOperations.indexOps(SemanticAssetMetadata.class);

        if (indexOps.exists()) {
            log.info("Index {} already exists, skipping creation.", INDEX_NAME);
            return;
        }

        List<String> synonyms = loadSynonymsFromClasspath()
                .orElse(Collections.emptyList());

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
                        "analysis", analysis));

        indexOps.create(indexSettings);
        indexOps.putMapping(indexOps.createMapping());
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


}
