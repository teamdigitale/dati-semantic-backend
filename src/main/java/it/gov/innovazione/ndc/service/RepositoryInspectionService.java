package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.harvester.SemanticAssetType;
import it.gov.innovazione.ndc.harvester.util.FileUtils;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ispezione di un repository GitHub via JGit (no chiamate REST a api.github.com):
 * <ul>
 *   <li><b>esistenza + visibilita'</b>: {@code git ls-remote} anonimo. Se torna {@code not authorized}/404
 *       il repo e' inesistente o privato (GitHub non distingue i due casi senza credenziali).</li>
 *   <li><b>layout per tipo</b>: shallow clone (depth=1), lettura della prima alberatura, e per ogni
 *       {@link SemanticAssetType} si verifica la presenza al root (layout legacy) o sotto {@code assets/}
 *       (layout corrente). Se nessuno dei 3 tipi e' presente, non e' un repo NDC.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RepositoryInspectionService {

    /** Directory contenitore del layout corrente: e' sempre l'inizio di {@code SemanticAssetType.folderName}. */
    private static final String CURRENT_CONTAINER = Arrays.stream(SemanticAssetType.values())
            .map(SemanticAssetType::getFolderName)
            .map(p -> p.split("/")[0])
            .findFirst()
            .orElse("assets");

    private final FileUtils fileUtils;

    public RepositoryInspection inspect(String url) {
        ParsedGithubUrl parsed = parseGithubUrl(url);
        if (parsed == null) {
            return RepositoryInspection.error(url, "URL non riconosciuta come repository GitHub");
        }

        Accessibility access = checkAccessible(url);
        if (access == Accessibility.NOT_FOUND_OR_PRIVATE) {
            return RepositoryInspection.builder()
                    .url(url)
                    .owner(parsed.owner)
                    .repo(parsed.repo)
                    .exists(false)
                    .isPublic(false)
                    .assetTypes(emptyDetection())
                    .build();
        }
        if (access == Accessibility.ERROR) {
            return RepositoryInspection.error(url,
                    "Errore nel contattare il repository (verifica connettivita' / rate limit)");
        }

        List<AssetTypeDetection> detections = probeAssetTypes(url, parsed);
        return RepositoryInspection.builder()
                .url(url)
                .owner(parsed.owner)
                .repo(parsed.repo)
                .exists(true)
                .isPublic(true)
                .assetTypes(detections)
                .build();
    }

    private enum Accessibility { OK, NOT_FOUND_OR_PRIVATE, ERROR }

    /**
     * Usa {@code git ls-remote} per discriminare esistenza/visibilita' senza
     * scaricare nulla. GitHub maschera i repo privati come "not authorized":
     * non potendo distinguere senza credenziali, li trattiamo come
     * {@link Accessibility#NOT_FOUND_OR_PRIVATE}.
     */
    private Accessibility checkAccessible(String url) {
        try {
            Collection<Ref> refs = Git.lsRemoteRepository().setRemote(url).call();
            return refs.isEmpty() ? Accessibility.NOT_FOUND_OR_PRIVATE : Accessibility.OK;
        } catch (TransportException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("not found")
                    || msg.contains("not authorized")
                    || msg.contains("authentication is required")) {
                return Accessibility.NOT_FOUND_OR_PRIVATE;
            }
            log.warn("ls-remote transport error for {}: {}", url, e.getMessage());
            return Accessibility.ERROR;
        } catch (Exception e) {
            log.warn("ls-remote unexpected error for {}: {}", url, e.getMessage());
            return Accessibility.ERROR;
        }
    }

    /**
     * Shallow clone (depth=1) della default branch in una temp dir, poi per ogni
     * {@link SemanticAssetType} verifica la presenza in posizione corrente
     * ({@code assets/<folder>}) o legacy ({@code <LegacyFolder>}) al root.
     */
    private List<AssetTypeDetection> probeAssetTypes(String url, ParsedGithubUrl parsed) {
        Path temp;
        try {
            temp = fileUtils.createTempDirectory("ndc-inspect-" + parsed.repo + "-");
        } catch (IOException e) {
            log.warn("Cannot create temp dir for inspect of {}: {}", url, e.getMessage());
            return emptyDetection();
        }
        try {
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(temp.toFile())
                    .setDepth(1)
                    .setCloneAllBranches(false)
                    .call()
                    .close();

            Set<String> rootDirs = listDirs(temp);
            Set<String> currentDirs = rootDirs.contains(CURRENT_CONTAINER)
                    ? listDirs(temp.resolve(CURRENT_CONTAINER))
                    : Set.of();

            List<AssetTypeDetection> out = new ArrayList<>();
            for (SemanticAssetType type : SemanticAssetType.values()) {
                out.add(detectType(type, rootDirs, currentDirs));
            }
            return out;
        } catch (Exception e) {
            log.warn("Shallow clone failed for {}: {}", url, e.getMessage());
            return emptyDetection();
        } finally {
            tryRemove(temp);
        }
    }

    private AssetTypeDetection detectType(
            SemanticAssetType type, Set<String> rootDirs, Set<String> currentDirs) {
        String currentLeaf = leaf(type.getFolderName());
        String legacyName = type.getLegacyFolderName();
        boolean hasCurrent = currentDirs.contains(currentLeaf);
        boolean hasLegacy = rootDirs.contains(legacyName);

        AssetTypeDetection.AssetTypeDetectionBuilder b = AssetTypeDetection.builder()
                .key(type.name())
                .label(type.toString());
        if (hasCurrent && hasLegacy) {
            return b.present(true).layout(Layout.MIXED)
                    .path(type.getFolderName() + " + " + legacyName)
                    .build();
        }
        if (hasCurrent) {
            return b.present(true).layout(Layout.CURRENT).path(type.getFolderName()).build();
        }
        if (hasLegacy) {
            return b.present(true).layout(Layout.LEGACY).path(legacyName).build();
        }
        return b.present(false).build();
    }

    private Set<String> listDirs(Path parent) {
        Set<String> dirs = new HashSet<>();
        if (!Files.isDirectory(parent)) {
            return dirs;
        }
        try (Stream<Path> stream = Files.list(parent)) {
            stream.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> !name.startsWith("."))
                    .forEach(dirs::add);
        } catch (IOException e) {
            log.warn("Cannot list dirs of {}: {}", parent, e.getMessage());
        }
        return dirs;
    }

    private static String leaf(String slashPath) {
        int idx = slashPath.lastIndexOf('/');
        return idx < 0 ? slashPath : slashPath.substring(idx + 1);
    }

    private static List<AssetTypeDetection> emptyDetection() {
        return Arrays.stream(SemanticAssetType.values())
                .map(t -> AssetTypeDetection.builder()
                        .key(t.name())
                        .label(t.toString())
                        .present(false)
                        .build())
                .collect(Collectors.toList());
    }

    private void tryRemove(Path path) {
        try {
            fileUtils.removeDirectory(path);
        } catch (IOException e) {
            log.warn("Cannot remove temp dir {}: {}", path, e.getMessage());
        }
    }

    @Nullable
    static ParsedGithubUrl parseGithubUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (uri.getHost() == null || !"github.com".equalsIgnoreCase(uri.getHost())) {
            return null;
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        List<String> parts = Arrays.stream(path.split("/"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        if (parts.size() < 2) {
            return null;
        }
        String owner = parts.get(0);
        String repo = parts.get(1).replaceAll("\\.git$", "");
        return new ParsedGithubUrl(owner, repo);
    }

    public record ParsedGithubUrl(String owner, String repo) {
    }

    public enum Layout {
        CURRENT,
        LEGACY,
        MIXED
    }

    @Value
    @Builder
    public static class AssetTypeDetection {
        /** {@link SemanticAssetType#name()}: ONTOLOGY, CONTROLLED_VOCABULARY, SCHEMA. */
        String key;
        /** Etichetta human-readable da {@link SemanticAssetType#toString()}. */
        String label;
        boolean present;
        /** {@code null} se assente. */
        Layout layout;
        /** Path rilevato sul filesystem (es. "assets/ontologies"), {@code null} se assente. */
        String path;
    }

    @Value
    @Builder
    public static class RepositoryInspection {
        String url;
        String owner;
        String repo;
        boolean exists;
        @com.fasterxml.jackson.annotation.JsonProperty("public")
        boolean isPublic;
        List<AssetTypeDetection> assetTypes;
        String error;

        static RepositoryInspection error(String url, String reason) {
            return RepositoryInspection.builder()
                    .url(url)
                    .exists(false)
                    .isPublic(false)
                    .assetTypes(emptyDetection())
                    .error(reason)
                    .build();
        }
    }
}
