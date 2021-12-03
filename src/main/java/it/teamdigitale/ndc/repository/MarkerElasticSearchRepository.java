package it.teamdigitale.ndc.repository;

import it.teamdigitale.ndc.harvester.model.index.SemanticAssetMetadata;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.stereotype.Repository;

// Marker so that spring boot creates index automatically
@Repository
@EnableElasticsearchRepositories(basePackages = "it.teamdigitale.ndc.repository")
public interface MarkerElasticSearchRepository
    extends ElasticsearchRepository<SemanticAssetMetadata, String> {
}
