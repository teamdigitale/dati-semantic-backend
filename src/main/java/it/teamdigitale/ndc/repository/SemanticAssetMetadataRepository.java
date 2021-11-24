package it.teamdigitale.ndc.repository;

import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemanticAssetMetadataRepository extends ElasticsearchRepository<SemanticAssetMetadata, String> {
}
