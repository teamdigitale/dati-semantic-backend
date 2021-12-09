package it.teamdigitale.ndc.harvester.harvesters;

import it.teamdigitale.ndc.harvester.SemanticAssetHarvester;
import it.teamdigitale.ndc.harvester.SemanticAssetType;
import it.teamdigitale.ndc.harvester.exception.SinglePathProcessingException;
import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseSemanticAssetHarvester<P extends SemanticAssetPath> implements SemanticAssetHarvester {
    private final SemanticAssetType type;

    @Override
    public SemanticAssetType getType() {
        return type;
    }

    @Override
    public void harvest(String repoUrl, Path rootPath) {
        log.debug("Looking for {} paths", type);

        List<P> paths = scanForPaths(rootPath);
        log.debug("Found {} {} path(s) for processing", paths.size(), type);

        for (P path : paths) {
            try {
                processPath(repoUrl, path);
                log.debug("Path {} processed correctly for {}", path, type);

            } catch (SinglePathProcessingException e) {
                log.error("Error processing {} {} in repo {}", type, path, repoUrl, e);
            }
        }
    }

    protected abstract void processPath(String repoUrl, P path);

    protected abstract List<P> scanForPaths(Path rootPath);
}
