package it.gov.innovazione.ndc.harvester.scanners;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import it.gov.innovazione.ndc.model.harvester.HarvesterRun;
import it.gov.innovazione.ndc.service.logging.HarvesterStage;
import it.gov.innovazione.ndc.service.logging.LoggingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static it.gov.innovazione.ndc.service.logging.NDCHarvesterLogger.logSemanticInfo;

@Component
@RequiredArgsConstructor
public class SchemaFolderScanner implements FolderScanner<SemanticAssetPath> {
    private final FileUtils fileUtils;

    private static boolean isIndex(Path path) {
        boolean isIndex = path.toString().toLowerCase(Locale.ROOT).endsWith("/index.ttl");
        if (!isIndex) {
            logSemanticInfo(LoggingContext.builder()
                    .stage(HarvesterStage.PATH_SCANNING)
                    .harvesterStatus(HarvesterRun.Status.RUNNING)
                    .additionalInfo("path", path.toString())
                    .message("Skipping file not named 'index.ttl'")
                    .build());
        }
        return isIndex;
    }

    @Override
    public List<SemanticAssetPath> scanFolder(Path folder) throws IOException {
        List<SemanticAssetPath> schemas = fileUtils.listContents(folder).stream()
                .filter(SchemaFolderScanner::isIndex)
                .map(path -> SemanticAssetPath.of(path.toString()))
                .toList();

        logSemanticInfo(LoggingContext.builder()
                .stage(HarvesterStage.PATH_SCANNING)
                .harvesterStatus(HarvesterRun.Status.RUNNING)
                .message("Scanned folder for schema files")
                .additionalInfo("folder", folder.toString())
                .additionalInfo("schemas", schemas.size())
                .build());

        return schemas;
    }
}
