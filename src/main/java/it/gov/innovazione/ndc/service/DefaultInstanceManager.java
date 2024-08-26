package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.Repository;
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
        configService.writeConfigKey(ACTIVE_INSTANCE, "system", getNextOnlineInstance(repository), repository.getId());

        // switch instance on Virtuoso
        tripleStoreRepository.switchInstances(repository);
    }

    public void rollbackInstance(Repository repository) {
        // rollback instance on Repositories
        configService.writeConfigKey(ACTIVE_INSTANCE, "system", getOldOnlineInstance(repository), repository.getId());
        // rollback instance on Virtuoso
        tripleStoreRepository.rollbackInstance(repository);
    }

    @Override
    public List<RepositoryInstance> getCurrentInstances() {
        return repositoryService.getActiveRepos().stream()
                .map(repo -> RepositoryInstance.of(repo.getUrl(), getCurrentInstance(repo)))
                .collect(Collectors.toList());
    }
}
