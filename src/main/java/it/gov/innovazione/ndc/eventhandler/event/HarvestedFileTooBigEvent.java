package it.gov.innovazione.ndc.eventhandler.event;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
@Data
public class HarvestedFileTooBigEvent implements AlertableEvent {
    private final String runId;
    private final Repository repository;
    private final String revision;
    private final ViolatingSemanticAsset violatingSemanticAsset;
    private final String relativePathInRepo;

    @Override
    public String getName() {
        return "HarvestedFileTooBig";
    }

    @Override
    public String getDescription() {
        return "A semantic asset file is too big";
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.SEMANTIC;
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }

    @Override
    public Map<String, Object> getContext() {
        return Map.of(
                "runId", runId,
                "repository", repository,
                "revision", revision,
                "violatingSemanticAsset", violatingSemanticAsset,
                "relativePathInRepo", relativePathInRepo);
    }

    @Data
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ViolatingSemanticAsset {
        private final String pathInRepo;
        private final Long maxFileSizeBytes;
        private final List<FileDetail> fileDetails;

        public static ViolatingSemanticAsset fromPath(
                String pathInRepo,
                List<File> files,
                Long maxFileSizeBytes) {
            return new ViolatingSemanticAsset(
                    pathInRepo,
                    maxFileSizeBytes,
                    files.stream()
                            .map(file -> new FileDetail(
                                    file.getPath(),
                                    file.length(),
                                    file.length() > maxFileSizeBytes))
                            .collect(Collectors.toList()));
        }
    }


    @Data
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FileDetail {
        private final String path;
        private final long size;
        private final boolean violatesMaxSize;
    }
}
