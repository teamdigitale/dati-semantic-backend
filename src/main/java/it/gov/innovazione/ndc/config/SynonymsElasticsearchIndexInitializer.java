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

    public static final String INDEX_NAME = "semantic-asset-metadata-8";
    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void createIndexWithSynonyms() throws Exception {

        IndexOperations indexOps = elasticsearchOperations.indexOps(SemanticAssetMetadata.class);

        if (indexOps.exists()) {
            return;
        }

        List<String> synonyms = loadSynonymsFromClasspath("synonyms.txt")
                .orElse(Collections.emptyList());

        Map<String, Object> settings = loadSettingsJson("elasticsearch-settings.json");

        Map<String, Object> analysis = (Map<String, Object>) settings.get("analysis");
        Map<String, Object> filter = (Map<String, Object>) analysis.get("filter");
        Map<String, Object> sinonimiFilter = (Map<String, Object>) filter.get("italian_synonyms");

        sinonimiFilter.put("synonyms", synonyms);

        indexOps.create(settings);
        indexOps.putMapping(indexOps.createMapping());
    }

    private Optional<List<String>> loadSynonymsFromClasspath(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), defaultCharset()))) {
                return Optional.of(reader.lines().collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.warn("Failed to load synonyms", e);
        }
        return Optional.empty();
    }

    private Map<String, Object> loadSettingsJson(String fileName) throws Exception {
        return objectMapper.readValue(new ClassPathResource(fileName).getInputStream(), Map.class);
    }


}
