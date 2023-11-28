package it.gov.innovazione.ndc.harvester;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@Data
@RequiredArgsConstructor(staticName = "of")
public class PathWithRevision {
    private final Path path;
    private final String revision;
}
