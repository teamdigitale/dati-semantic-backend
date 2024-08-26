package it.gov.innovazione.ndc.harvester.context;

import it.gov.innovazione.ndc.harvester.model.Instance;
import it.gov.innovazione.ndc.harvester.model.index.RightsHolder;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.With;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.harvesters.utils.PathUtils.relativizeFile;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HarvestExecutionContext {
    private final Repository repository;
    private final String revision;
    private final String correlationId;
    private final String runId;
    private final String currentUserId;
    @With
    private final String rootPath;
    @With
    private final Instance instance;
    @Singular
    private final List<Repository.Maintainer> maintainers = new ArrayList<>();
    @Singular
    private final List<RightsHolder> rightsHolders = new ArrayList<>();
    @Singular
    private final List<HarvesterExecutionError> errors = new ArrayList<>();

    public void addRightsHolder(RightsHolder agencyId) {
        rightsHolders.add(agencyId);
    }

    public void addHarvestingError(
            Repository repository,
            Exception e,
            List<File> allFiles) {
        List<String> relativizedFiles = allFiles.stream()
                .map(f -> relativizeFile(f.getAbsolutePath(), this))
                .collect(Collectors.toList());
        errors.add(HarvesterExecutionError.of(repository, e.getCause(), relativizedFiles));
    }

    public void addMaintainers(List<Repository.Maintainer> maintainers) {
        this.maintainers.addAll(maintainers);
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class HarvesterExecutionError {
        private final Repository repository;
        private final Throwable exception;
        private final List<String> files;
    }
}
