package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.repository.SemanticAssetMetadataDeleter;
import it.gov.innovazione.ndc.repository.TripleStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.ConfigKey.ACTIVE_INSTANCE;

@Service
@RequiredArgsConstructor
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
        Instance newInstance = getNextOnlineInstance(repository);
        configService.writeConfigKey(ACTIVE_INSTANCE, "system", newInstance, repository.getId());

        Instance instanceToDelete = newInstance.switchInstance();

        deleter.deleteByRepoUrl(repository.getUrl(), instanceToDelete);

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
