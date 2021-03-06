package it.gov.innovazione.ndc.harvester.scanners;

import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FolderScanner<P extends SemanticAssetPath> {
    String TURTLE_FILE_EXTENSION = ".ttl";

    List<P> scanFolder(Path folder) throws IOException;
}
