package it.gov.innovazione.ndc.repository;

import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.stereotype.Repository;

// Marker so that spring boot creates index automatically
@Repository
@EnableElasticsearchRepositories(basePackages = "it.gov.innovazione.ndc.repository")
public interface MarkerElasticSearchRepository
    extends ElasticsearchRepository<SemanticAssetMetadata, String> {
}
