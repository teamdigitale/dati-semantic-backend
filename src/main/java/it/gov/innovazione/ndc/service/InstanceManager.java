package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

public interface InstanceManager {
    Instance getNextOnlineInstance(String repoUrl);

    Instance getNextOnlineInstance(Repository repository);

    Instance getCurrentInstance(Repository repository);

    void switchInstances(Repository repository);

    void rollbackInstance(Repository repository);

    List<RepositoryInstance> getCurrentInstances();

    @Data
    @RequiredArgsConstructor(staticName = "of")
    class RepositoryInstance {
        public final String url;
        public final Instance instance;
    }
}
