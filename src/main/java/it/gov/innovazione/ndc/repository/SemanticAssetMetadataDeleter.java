package it.gov.innovazione.ndc.repository;

import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.index.SemanticAssetMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticAssetMetadataDeleter {
    private final ElasticsearchOperations esOps;

    public long deleteByRepoUrl(String repoUrl, Instance instance) {
        return esOps.delete(
                SemanticAssetMetadataQuery.getDeleteQueryBuilder(repoUrl, instance)
                        .build(),
                SemanticAssetMetadata.class).getDeleted();
    }
}
