package it.teamdigitale.ndc.harvester;

import it.teamdigitale.ndc.harvester.model.SemanticAssetPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FolderScanner<P extends SemanticAssetPath> {
    List<P> scanFolder(Path folder) throws IOException;
}
