package it.gov.innovazione.ndc.service;


import it.gov.innovazione.ndc.config.HarvestExecutionContext;
import it.gov.innovazione.ndc.config.HarvestExecutionContextUtils;
import it.gov.innovazione.ndc.harvester.service.ActualConfigService;
import it.gov.innovazione.ndc.harvester.service.ConfigService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubService {

    private final GitHub gitHub;
    private final ConfigService configService;
    private final RepositoryService repositoryService;
    private final View error;

    public static Optional<OwnerRepo> extractOwnerRepo(String url) {
        try {
            URI uri = new URI(url);

            if (!StringUtils.contains(uri.getHost(), "github.com")) {
                log.warn("Only GitHub URLs are supported, got {} instead", url);
                return Optional.empty();
            }

            String path = uri.getPath();

            // Remove leading slash if present
            path = path.startsWith("/") ? path.substring(1) : path;

            String[] segments = path.split("/");

            if (segments.length == 2) {
                return Optional.of(OwnerRepo.of(segments[0], segments[1]));
            }
            log.warn("Invalid URL: {}, it doesn't contain owner/repo", url);
        } catch (URISyntaxException e) {
            log.error("Error parsing URL", e);
        }
        return Optional.empty();
    }

    public Optional<GHIssue> getNdcIssueIfPresent(String repoUrl) {
        Optional<GHIssue> ndcIssue = getOpenedIssues(repoUrl).stream()
                .filter(this::isNdc)
                .findAny();

        if (ndcIssue.isPresent()) {
            log.info("Repo {} contains an ndc issue #{}", repoUrl, ndcIssue.get().getNumber());
            return ndcIssue;
        }
        log.info("ndc issues not detected on repo {}", repoUrl);
        return Optional.empty();
    }

    @SneakyThrows
    private boolean isNdc(GHIssue issue) {
        String login = gitHub.getMyself().getLogin();
        String user = issue.getUser().getLogin();
        String title = issue.getTitle().toLowerCase();

        boolean isNdc = title.contains("[schemagov]") && user.equalsIgnoreCase(login);

        if (isNdc) {
            log.info("issue #{} on repo {} is NDC -- title {}, user {}",
                    issue.getNumber(),
                    issue.getRepository().getUrl(),
                    title,
                    user);
        }
        return isNdc;
    }

    private List<GHIssue> getOpenedIssues(String repoUrl) {
        try {
            if (!isEnabled(repoUrl)) {
                log.warn("GitHub service is not enabled, or repository is not found");
                return List.of();
            }
            Optional<String> ownerRepo = extractOwnerRepo(repoUrl)
                    .map(OwnerRepo::toString);

            if (ownerRepo.isEmpty()) {
                log.warn("This URL [{}] looks invalid, cannot use GitHubService on it", repoUrl);
                return List.of();
            }

            GHRepository repository = gitHub.getRepository(ownerRepo.get());
            return repository.getIssues(GHIssueState.OPEN);

        } catch (Exception e) {
            log.error("Error getting issues", e);
            return List.of();
        }
    }

    private boolean isEnabled(String repoUrl) {
        Optional<Repository> repository = repositoryService.findActiveRepoByUrl(repoUrl);

        if (repository.isEmpty()) {
            log.warn("Repository {} not found", repoUrl);
            return false;
        }

        return configService.getFromRepoOrGlobalOrDefault(
                ActualConfigService.ConfigKey.GITHUB_ISSUER_ENABLED,
                repository.get().getId(),
                false);
    }

    public void openIssueIfNecessary() {
        try {
            if (!isEnabled(HarvestExecutionContextUtils.getContext().getRepository().getUrl())) {
                log.warn("GitHub service is not enabled, skipping issue creation");
                return;
            }
            HarvestExecutionContext context = HarvestExecutionContextUtils.getContext();
            List<HarvestExecutionContext.HarvesterExecutionError> errors = context.getErrors();
            if (errors.isEmpty()) {
                log.info("No errors detected on repository {}, skipping issue creation",
                        context.getRepository().getUrl());
                return;
            }
            String repoUrl = context.getRepository().getUrl();
            Optional<String> ownerRepo = extractOwnerRepo(repoUrl)
                    .map(OwnerRepo::toString);

            if (ownerRepo.isEmpty()) {
                log.warn("This URL [{}] looks invalid, cannot use GitHubService on it", repoUrl);
                return;
            }

            GHRepository repository = gitHub.getRepository(ownerRepo.get());

            String issueBody = Stream.of(
                    IntStream.range(0, errors.size())
                            .map(i -> i + 1)
                            .mapToObj(i -> asIssueBody(i, errors.get(i - 1)))
                            .collect(Collectors.joining("\n\n")),
                    "---",
                    "**Origin**: Automatically opened by the harvester",
                    "**runId**: " + context.getRunId(),
                    "").collect(joining("\n"));

            String issueTitle =
                    format("[SCHEMAGOV] Error processing Semantic Assets (%s)", context.getRunId());

            repository.createIssue(issueTitle)
                    .body(issueBody)
                    .label("ndc")
                    .create();

        } catch (Exception e) {
            log.error("Error creating issue", e);
        }
    }

    private String asIssueBody(int i, HarvestExecutionContext.HarvesterExecutionError error) {
        return Stream.of(
                        format("### %d. Exception in File: [%s](%s) ", i,
                                getFile(error),
                                getFileUrl(error)),
                        format("   - **Description**: %s", error.getException().getMessage()),
                        format("   - **Date and Time**: %s", LocalDateTime.now()),
                        "",
                        "---")
                .collect(joining("\n"));
    }

    private String getFile(HarvestExecutionContext.HarvesterExecutionError error) {
        return error.getFiles().size() == 1 ? error.getFiles().get(0) :
                error.getFiles()
                        .stream()
                        .filter(f -> StringUtils.endsWithIgnoreCase(f, ".csv"))
                        .findFirst()
                        .orElse(error.getFiles().get(0));
    }

    private String getFileUrl(HarvestExecutionContext.HarvesterExecutionError error) {
        String repoUrl = HarvestExecutionContextUtils.getContext().getRepository().getUrl();
        return format("%s/blob/%s/%s", repoUrl,
                HarvestExecutionContextUtils.getContext().getRevision(),
                getFile(error));
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    public static class OwnerRepo {
        private final String owner;
        private final String repo;

        @Override
        public String toString() {
            return owner + "/" + repo;
        }
    }
}
