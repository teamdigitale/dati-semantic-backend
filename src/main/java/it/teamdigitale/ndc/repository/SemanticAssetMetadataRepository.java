package it.teamdigitale.ndc.repository;

import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SemanticAssetMetadataRepository extends ElasticsearchRepository<SemanticAssetMetadata, String> {
}
