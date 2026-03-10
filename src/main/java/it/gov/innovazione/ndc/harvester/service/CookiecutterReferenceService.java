package it.gov.innovazione.ndc.harvester.service;

import it.gov.innovazione.ndc.service.NdcGitHubClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CookiecutterReferenceService {

    private final NdcGitHubClient gitHubClient;

    @Value("${harvester.conformance.cookiecutter-repo:teamdigitale/dati-semantic-cookiecutter}")
    private String cookiecutterRepo;

    @Value("${harvester.conformance.cookiecutter-branch:main}")
    private String cookiecutterBranch;

    @Value("${harvester.conformance.enabled:true}")
    private boolean enabled;

    private final AtomicReference<CookiecutterReference> cachedReference = new AtomicReference<>();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]+)\"");

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<CookiecutterReference> getReference() {
        CookiecutterReference cached = cachedReference.get();
        if (cached != null) {
            return Optional.of(cached);
        }
        return loadReference();
    }

    public void invalidateCache() {
        cachedReference.set(null);
    }

    private Optional<CookiecutterReference> loadReference() {
        if (gitHubClient.getEnabled()) {
            return loadViaGitHubClient();
        }
        return loadViaPublicApi();
    }

    private Optional<CookiecutterReference> loadViaGitHubClient() {
        try {
            GitHub gitHub = gitHubClient.getGitHub();
            GHRepository repo = gitHub.getRepository(cookiecutterRepo);

            String validateYaml = fetchFileContentViaClient(repo, ".github/workflows/validate.yaml");
            String preCommitConfig = fetchFileContentViaClient(repo, ".pre-commit-config.yaml");
            return buildAndCacheReference(validateYaml, preCommitConfig);
        } catch (IOException e) {
            log.error("Failed to load cookiecutter reference via GitHub client from {}", cookiecutterRepo, e);
            return loadViaPublicApi();
        }
    }

    private Optional<CookiecutterReference> loadViaPublicApi() {
        try {
            log.info("Loading cookiecutter reference via public GitHub API from {}", cookiecutterRepo);
            String validateYaml = fetchFileContentViaPublicApi(".github/workflows/validate.yaml");
            String preCommitConfig = fetchFileContentViaPublicApi(".pre-commit-config.yaml");
            return buildAndCacheReference(validateYaml, preCommitConfig);
        } catch (Exception e) {
            log.error("Failed to load cookiecutter reference via public API from {}", cookiecutterRepo, e);
            return Optional.empty();
        }
    }

    private Optional<CookiecutterReference> buildAndCacheReference(String validateYaml, String preCommitConfig) {
        List<String> semanticHooks = extractSemanticHooks(preCommitConfig);

        CookiecutterReference reference = CookiecutterReference.builder()
                .validateYaml(validateYaml)
                .preCommitConfig(preCommitConfig)
                .semanticHooks(semanticHooks)
                .build();

        cachedReference.set(reference);
        log.info("Loaded cookiecutter reference from {}/{} with {} semantic hooks",
                cookiecutterRepo, cookiecutterBranch, semanticHooks.size());
        return Optional.of(reference);
    }

    private String fetchFileContentViaClient(GHRepository repo, String path) throws IOException {
        try {
            GHContent content = repo.getFileContent(path, cookiecutterBranch);
            return new String(content.read().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Could not fetch {} from {}: {}", path, cookiecutterRepo, e.getMessage());
            return null;
        }
    }

    private String fetchFileContentViaPublicApi(String path) {
        try {
            String url = String.format("https://api.github.com/repos/%s/contents/%s?ref=%s",
                    cookiecutterRepo, path, cookiecutterBranch);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Could not fetch {} from {}: HTTP {}", path, cookiecutterRepo, response.statusCode());
                return null;
            }

            Matcher matcher = CONTENT_PATTERN.matcher(response.body());
            if (matcher.find()) {
                String base64Content = matcher.group(1).replace("\\n", "");
                return new String(Base64.getMimeDecoder().decode(base64Content), StandardCharsets.UTF_8);
            }
            log.warn("Could not parse content from GitHub API response for {}", path);
            return null;
        } catch (Exception e) {
            log.warn("Could not fetch {} from {}: {}", path, cookiecutterRepo, e.getMessage());
            return null;
        }
    }

    static List<String> extractSemanticHooks(String preCommitConfig) {
        if (preCommitConfig == null || preCommitConfig.isBlank()) {
            return Collections.emptyList();
        }
        return preCommitConfig.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("- id:"))
                .map(line -> line.substring("- id:".length()).trim())
                .filter(id -> id.startsWith("validate-") || id.equals("validate-csv"))
                .toList();
    }

    @Getter
    @lombok.Builder
    public static class CookiecutterReference {
        private final String validateYaml;
        private final String preCommitConfig;
        private final List<String> semanticHooks;
    }
}
