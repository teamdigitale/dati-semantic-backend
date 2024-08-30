package it.gov.innovazione.ndc.integration;

import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.model.harvester.Repository;
import it.gov.innovazione.ndc.service.InstanceManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Primary
@RequiredArgsConstructor
public class InMemoryInstanceManager implements InstanceManager {

    private final Map<String, Instance> instances = new HashMap<>();

    private Instance allInstances = Instance.PRIMARY;

    public void setAllInstances(Instance allInstances) {
        this.allInstances = allInstances;
        instances.replaceAll((k, v) -> allInstances);
    }

    @Override
    public Instance getNextOnlineInstance(String repoUrl) {
        if (instances.containsKey(repoUrl)) {
            return instances.get(repoUrl).switchInstance();
        }
        Repository fakeRepo = Repository.builder()
                .url(repoUrl)
                .build();
        return getCurrentInstance(fakeRepo).switchInstance();
    }

    @Override
    public Instance getNextOnlineInstance(Repository repository) {
        return getCurrentInstance(repository).switchInstance();
    }

    @Override
    public Instance getCurrentInstance(Repository repository) {
        if (!instances.containsKey(repository.getUrl())) {
            instances.put(repository.getUrl(), allInstances);
        }
        return instances.get(repository.getUrl());
    }

    @Override
    public void switchInstances(Repository repository) {
        Instance toPut = getCurrentInstance(repository).switchInstance();
        instances.put(repository.getUrl(), toPut);
    }

    @Override
    public List<RepositoryInstance> getCurrentInstances() {
        return instances.entrySet().stream()
                .map(entry -> RepositoryInstance.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public void switchAllInstances() {
        instances.replaceAll((k, v) -> v.switchInstance());
    }
}
