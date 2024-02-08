package it.gov.innovazione.ndc.eventhandler.event;

import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
public class HarvestedFileTooBigEvent {
    private final String runId;
    private final Repository repository;
    private final String revision;
    private final ViolatingSemanticAsset violatingSemanticAsset;
    private final String relativePathInRepo;

    @Data
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ViolatingSemanticAsset {
        private final String pathInRepo;
        private final List<FileDetail> fileDetails;

        public static ViolatingSemanticAsset fromPath(
                String pathInRepo,
                List<File> files,
                long maxFileSizeBytes) {
            return new ViolatingSemanticAsset(
                    pathInRepo,
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
