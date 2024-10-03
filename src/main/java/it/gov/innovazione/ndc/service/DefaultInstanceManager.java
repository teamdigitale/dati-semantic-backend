package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataDeleter;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.ConfigKey.ACTIVE_INSTANCE;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultInstanceManager implements InstanceManager {

    private final ConfigService configService;
    private final RepositoryService repositoryService;
    private final TripleStoreRepository tripleStoreRepository;
    private final SemanticAssetMetadataDeleter deleter;

    public Instance getNextOnlineInstance(String repoUrl) {
        Optional<Repository> repository = repositoryService.findActiveRepoByUrl(repoUrl);
        if (repository.isEmpty()) {
            throw new IllegalArgumentException("Repository not found for url: " + repoUrl);
        }
        return getNextOnlineInstance(repository.get());
    }

    public Instance getNextOnlineInstance(Repository repository) {
        return getCurrentInstance(repository).switchInstance();
    }

    public Instance getOldOnlineInstance(Repository repository) {
        return getCurrentInstance(repository).switchInstance();
    }

    public Instance getCurrentInstance(Repository repository) {
        Optional<Instance> instance = configService.fromRepo(ACTIVE_INSTANCE, repository.getId());
        return instance.orElse(Instance.PRIMARY);
    }

    public void switchInstances(Repository repository) {
        // switch instance on Repositories
        log.info("Switching instance for repository {}", repository.getUrl());
        Instance newInstance = getNextOnlineInstance(repository);

        log.info("Switching Elastic search to instance {} for repo {}", newInstance, repository.getUrl());

        configService.writeConfigKey(ACTIVE_INSTANCE, "system", newInstance, repository.getId());

        Instance instanceToDelete = newInstance.switchInstance();

        log.info("Deleting metadata for instance {} for repo {}", instanceToDelete, repository.getUrl());

        deleter.deleteByRepoUrl(repository.getUrl(), instanceToDelete);

        log.info("Switching instances on Virtuoso for repo {}", repository.getUrl());

        // switch instance on Virtuoso
        tripleStoreRepository.switchInstances(repository);
    }

    @Override
    public List<RepositoryInstance> getCurrentInstances() {
        return repositoryService.getActiveRepos().stream()
                .map(repo -> RepositoryInstance.of(repo.getUrl(), getCurrentInstance(repo)))
                .collect(Collectors.toList());
    }
}
