package it.teamdigitale.ndc.repository;

import it.teamdigitale.ndc.harvester.model.SemanticAssetMetadata;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SemanticAssetMetadataRepository
    extends ElasticsearchRepository<SemanticAssetMetadata, String> {

    @Query("{\"match\": {\"searchableText\": {\"query\": \"?0\"}}}")
    Page<SemanticAssetMetadata> findBySearchableText(String searchTerm,
                                                     Pageable pageable);

    Optional<SemanticAssetMetadata> findByIri(String iri);
}
